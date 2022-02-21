/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.transformSuspendFunctionToRuntimeFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlin.utils.DO_NOTHING_3

interface TypeMappingConfiguration<out T : Any> {
    fun commonSupertype(types: Collection<@JvmSuppressWildcards KotlinType>): KotlinType
    fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): T?
    fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String?
    fun getPredefinedFullInternalNameForClass(classDescriptor: ClassDescriptor): String? = null
    fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor)

    // returns null when type doesn't need to be preprocessed
    fun preprocessType(kotlinType: KotlinType): KotlinType? = null
}

fun <T : Any> mapType(
    kotlinType: KotlinType,
    factory: JvmTypeFactory<T>,
    mode: TypeMappingMode,
    typeMappingConfiguration: TypeMappingConfiguration<T>,
    descriptorTypeWriter: JvmDescriptorTypeWriter<T>?,
    writeGenericType: (KotlinType, T, TypeMappingMode) -> Unit = DO_NOTHING_3
): T {
    typeMappingConfiguration.preprocessType(kotlinType)?.let { newType ->
        return mapType(newType, factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType)
    }

    if (kotlinType.isSuspendFunctionType) {
        return mapType(
            transformSuspendFunctionToRuntimeFunctionType(kotlinType),
            factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType
        )
    }

    with(SimpleClassicTypeSystemContext) {
        mapBuiltInType(kotlinType, factory, mode)
    }?.let { builtInType ->
        val jvmType = factory.boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing)
        writeGenericType(kotlinType, jvmType, mode)
        return jvmType
    }

    val constructor = kotlinType.constructor
    if (constructor is IntersectionTypeConstructor) {
        val intersectionType = constructor.getAlternativeType()
            ?: typeMappingConfiguration.commonSupertype(constructor.supertypes)
        // interface In<in E>
        // open class A : In<A>
        // open class B : In<B>
        // commonSupertype(A, B) = In<A & B>
        // So replace arguments with star-projections to prevent infinite recursive mapping
        // It's not very important because such types anyway are prohibited in declarations
        return mapType(
            intersectionType.replaceArgumentsWithStarProjections(),
            factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType
        )
    }

    val descriptor =
        constructor.declarationDescriptor
            ?: throw UnsupportedOperationException("no descriptor for type constructor of $kotlinType")

    when {
        ErrorUtils.isError(descriptor) -> {
            val jvmType = factory.createObjectType(NON_EXISTENT_CLASS_NAME)
            typeMappingConfiguration.processErrorType(kotlinType, descriptor as ClassDescriptor)
            descriptorTypeWriter?.writeClass(jvmType)
            return jvmType
        }

        descriptor is ClassDescriptor && KotlinBuiltIns.isArray(kotlinType) -> {
            if (kotlinType.arguments.size != 1) {
                throw UnsupportedOperationException("arrays must have one type argument")
            }
            val memberProjection = kotlinType.arguments[0]
            val memberType = memberProjection.type

            val arrayElementType: T
            if (memberProjection.projectionKind === Variance.IN_VARIANCE) {
                arrayElementType = factory.createObjectType("java/lang/Object")
                descriptorTypeWriter?.apply {
                    writeArrayType()
                    writeClass(arrayElementType)
                    writeArrayEnd()
                }
            } else {
                descriptorTypeWriter?.writeArrayType()

                arrayElementType = mapType(
                    memberType, factory, mode.toGenericArgumentMode(memberProjection.projectionKind, ofArray = true),
                    typeMappingConfiguration, descriptorTypeWriter, writeGenericType
                )

                descriptorTypeWriter?.writeArrayEnd()
            }

            return factory.createFromString("[" + factory.toString(arrayElementType))
        }

        descriptor is ClassDescriptor -> {
            // NB if inline class is recursive, it's ok to map it as wrapped
            if (descriptor.isInlineClass() && !mode.needInlineClassWrapping) {
                val expandedType = SimpleClassicTypeSystemContext.computeExpandedTypeForInlineClass(kotlinType) as KotlinType?
                if (expandedType != null) {
                    return mapType(
                        expandedType, factory, mode.wrapInlineClassesMode(), typeMappingConfiguration,
                        descriptorTypeWriter, writeGenericType
                    )
                }
            }

            val jvmType =
                if (mode.isForAnnotationParameter && KotlinBuiltIns.isKClass(descriptor)) {
                    factory.javaLangClassType
                } else {
                    typeMappingConfiguration.getPredefinedTypeForClass(descriptor.original)
                        ?: run {
                            // refer to enum entries by enum type in bytecode unless ASM_TYPE is written
                            val enumClassIfEnumEntry =
                                if (descriptor.kind == ClassKind.ENUM_ENTRY)
                                    descriptor.containingDeclaration as ClassDescriptor
                                else
                                    descriptor
                            factory.createObjectType(computeInternalName(enumClassIfEnumEntry.original, typeMappingConfiguration))
                        }
                }

            writeGenericType(kotlinType, jvmType, mode)

            return jvmType
        }

        descriptor is TypeParameterDescriptor -> {
            val upperBound = descriptor.representativeUpperBound
            val type = mapType(
                if (kotlinType.isMarkedNullable) upperBound.makeNullable() else upperBound,
                factory, mode, typeMappingConfiguration,
                writeGenericType = DO_NOTHING_3, descriptorTypeWriter = null
            )
            descriptorTypeWriter?.writeTypeVariable(descriptor.getName(), type)
            return type
        }

        descriptor is TypeAliasDescriptor && mode.mapTypeAliases -> {
            return mapType(descriptor.expandedType, factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType)
        }

        else -> throw UnsupportedOperationException("Unknown type $kotlinType")
    }
}

fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

fun computeInternalName(
    klass: ClassDescriptor,
    typeMappingConfiguration: TypeMappingConfiguration<*> = TypeMappingConfigurationImpl
): String {
    typeMappingConfiguration.getPredefinedFullInternalNameForClass(klass)?.let { return it }

    val container = klass.containingDeclaration

    val name = SpecialNames.safeIdentifier(klass.name).identifier
    if (container is PackageFragmentDescriptor) {
        val fqName = container.fqName
        return if (fqName.isRoot) name else fqName.asString().replace('.', '/') + '/' + name
    }

    val containerClass = container as? ClassDescriptor
        ?: throw IllegalArgumentException("Unexpected container: $container for $klass")

    val containerInternalName =
        typeMappingConfiguration.getPredefinedInternalNameForClass(containerClass)
            ?: computeInternalName(containerClass, typeMappingConfiguration)

    return "$containerInternalName$$name"
}
