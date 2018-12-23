/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.utils.DO_NOTHING_3

interface JvmTypeFactory<T : Any> {
    fun boxType(possiblyPrimitiveType: T): T
    fun createFromString(representation: String): T
    fun createObjectType(internalName: String): T
    fun toString(type: T): String

    val javaLangClassType: T
}

private fun <T : Any> JvmTypeFactory<T>.boxTypeIfNeeded(possiblyPrimitiveType: T, needBoxedType: Boolean) =
    if (needBoxedType) boxType(possiblyPrimitiveType) else possiblyPrimitiveType

interface TypeMappingConfiguration<out T : Any> {
    fun commonSupertype(types: Collection<@JvmSuppressWildcards KotlinType>): KotlinType
    fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): T?
    fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String?
    fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor)
    // returns null when type doesn't need to be preprocessed
    fun preprocessType(kotlinType: KotlinType): KotlinType? = null

    fun releaseCoroutines(): Boolean
}

const val NON_EXISTENT_CLASS_NAME = "error/NonExistentClass"

fun <T : Any> mapType(
    kotlinType: KotlinType,
    factory: JvmTypeFactory<T>,
    mode: TypeMappingMode,
    typeMappingConfiguration: TypeMappingConfiguration<T>,
    descriptorTypeWriter: JvmDescriptorTypeWriter<T>?,
    writeGenericType: (KotlinType, T, TypeMappingMode) -> Unit = DO_NOTHING_3,
    isIrBackend: Boolean
): T {
    typeMappingConfiguration.preprocessType(kotlinType)?.let { newType ->
        return mapType(newType, factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType, isIrBackend)
    }

    if (kotlinType.isSuspendFunctionType) {
        return mapType(
            transformSuspendFunctionToRuntimeFunctionType(kotlinType, typeMappingConfiguration.releaseCoroutines()),
            factory, mode, typeMappingConfiguration, descriptorTypeWriter,
            writeGenericType,
            isIrBackend
        )
    }

    mapBuiltInType(kotlinType, factory, mode)?.let { builtInType ->
        val jvmType = factory.boxTypeIfNeeded(builtInType, mode.needPrimitiveBoxing)
        writeGenericType(kotlinType, jvmType, mode)
        return jvmType
    }

    val constructor = kotlinType.constructor
    if (constructor is IntersectionTypeConstructor) {
        val commonSupertype = typeMappingConfiguration.commonSupertype(constructor.supertypes)
        // interface In<in E>
        // open class A : In<A>
        // open class B : In<B>
        // commonSupertype(A, B) = In<A & B>
        // So replace arguments with star-projections to prevent infinite recursive mapping
        // It's not very important because such types anyway are prohibited in declarations
        return mapType(
            commonSupertype.replaceArgumentsWithStarProjections(),
            factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType, isIrBackend
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

                arrayElementType =
                        mapType(
                            memberType, factory,
                            mode.toGenericArgumentMode(memberProjection.projectionKind),
                            typeMappingConfiguration, descriptorTypeWriter, writeGenericType,
                            isIrBackend
                        )

                descriptorTypeWriter?.writeArrayEnd()
            }

            return factory.createFromString("[" + factory.toString(arrayElementType))
        }

        descriptor is ClassDescriptor -> {
            // NB if inline class is recursive, it's ok to map it as wrapped
            if (descriptor.isInline && !mode.needInlineClassWrapping) {
                val expandedType = computeExpandedTypeForInlineClass(kotlinType)
                if (expandedType != null) {
                    return mapType(
                        expandedType,
                        factory,
                        mode.wrapInlineClassesMode(),
                        typeMappingConfiguration,
                        descriptorTypeWriter,
                        writeGenericType,
                        isIrBackend
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
                            factory.createObjectType(
                                computeInternalName(
                                    enumClassIfEnumEntry.original,
                                    typeMappingConfiguration,
                                    isIrBackend
                                )
                            )
                        }
                }

            writeGenericType(kotlinType, jvmType, mode)

            return jvmType
        }

        descriptor is TypeParameterDescriptor -> {
            val type = mapType(
                getRepresentativeUpperBound(descriptor),
                factory,
                mode,
                typeMappingConfiguration,
                writeGenericType = DO_NOTHING_3,
                descriptorTypeWriter = null,
                isIrBackend = isIrBackend
            )
            descriptorTypeWriter?.writeTypeVariable(descriptor.getName(), type)
            return type
        }

        else -> throw UnsupportedOperationException("Unknown type $kotlinType")
    }
}


fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

private fun continuationInternalName(releaseCoroutines: Boolean): String {
    val fqName =
        if (releaseCoroutines) DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE
        else DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_EXPERIMENTAL
    return JvmClassName.byClassId(ClassId.topLevel(fqName)).internalName
}

private fun <T : Any> mapBuiltInType(
    type: KotlinType,
    typeFactory: JvmTypeFactory<T>,
    mode: TypeMappingMode
): T? {
    val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null

    if (descriptor === FAKE_CONTINUATION_CLASS_DESCRIPTOR_EXPERIMENTAL) {
        return typeFactory.createObjectType(continuationInternalName(false))
    } else if (descriptor == FAKE_CONTINUATION_CLASS_DESCRIPTOR_RELEASE) {
        return typeFactory.createObjectType(continuationInternalName(true))
    }

    val primitiveType = KotlinBuiltIns.getPrimitiveType(descriptor)
    if (primitiveType != null) {
        val jvmType = typeFactory.createFromString(JvmPrimitiveType.get(primitiveType).desc)
        val isNullableInJava = TypeUtils.isNullableType(type) || type.hasEnhancedNullability()
        return typeFactory.boxTypeIfNeeded(jvmType, isNullableInJava)
    }

    val arrayElementType = KotlinBuiltIns.getPrimitiveArrayType(descriptor)
    if (arrayElementType != null) {
        return typeFactory.createFromString("[" + JvmPrimitiveType.get(arrayElementType).desc)
    }

    if (KotlinBuiltIns.isUnderKotlinPackage(descriptor)) {
        val classId = JavaToKotlinClassMap.mapKotlinToJava(descriptor.fqNameUnsafe)
        if (classId != null) {
            if (!mode.kotlinCollectionsToJavaCollections &&
                JavaToKotlinClassMap.mutabilityMappings.any { it.javaClass == classId }
            ) return null

            return typeFactory.createObjectType(JvmClassName.byClassId(classId).internalName)
        }
    }

    return null
}

internal fun computeUnderlyingType(inlineClassType: KotlinType): KotlinType? {
    if (!shouldUseUnderlyingType(inlineClassType)) return null

    val descriptor = inlineClassType.unsubstitutedUnderlyingType()?.constructor?.declarationDescriptor ?: return null
    return if (descriptor is TypeParameterDescriptor)
        getRepresentativeUpperBound(descriptor)
    else
        inlineClassType.substitutedUnderlyingType()
}

internal fun computeExpandedTypeForInlineClass(inlineClassType: KotlinType): KotlinType? =
    computeExpandedTypeInner(inlineClassType, hashSetOf())

internal fun computeExpandedTypeInner(kotlinType: KotlinType, visitedClassifiers: HashSet<ClassifierDescriptor>): KotlinType? {
    val classifier = kotlinType.constructor.declarationDescriptor
        ?: throw AssertionError("Type with a declaration expected: $kotlinType")
    if (!visitedClassifiers.add(classifier)) return null

    return when {
        classifier is TypeParameterDescriptor ->
            computeExpandedTypeInner(getRepresentativeUpperBound(classifier), visitedClassifiers)
                ?.let { expandedUpperBound ->
                    if (expandedUpperBound.isNullable() || !kotlinType.isMarkedNullable)
                        expandedUpperBound
                    else
                        expandedUpperBound.makeNullable()
                }

        classifier is ClassDescriptor && classifier.isInline -> {
            val inlineClassBoxType = kotlinType

            val underlyingType = kotlinType.substitutedUnderlyingType() ?: return null
            val expandedUnderlyingType = computeExpandedTypeInner(underlyingType, visitedClassifiers) ?: return null
            when {
                !kotlinType.isNullable() -> expandedUnderlyingType

                // Here inline class type is nullable. Apply nullability to the expandedUnderlyingType.

                // Nullable types become inline class boxes
                expandedUnderlyingType.isNullable() -> inlineClassBoxType

                // Primitives become inline class boxes
                KotlinBuiltIns.isPrimitiveType(expandedUnderlyingType) -> inlineClassBoxType

                // Non-null reference types become nullable reference types
                else -> expandedUnderlyingType.makeNullable()
            }
        }

        else -> kotlinType
    }
}

internal fun shouldUseUnderlyingType(inlineClassType: KotlinType): Boolean {
    val underlyingType = inlineClassType.unsubstitutedUnderlyingType() ?: return false

    return !inlineClassType.isMarkedNullable ||
            !TypeUtils.isNullableType(underlyingType) && !KotlinBuiltIns.isPrimitiveType(underlyingType)
}

fun computeInternalName(
    klass: ClassDescriptor,
    typeMappingConfiguration: TypeMappingConfiguration<*> = TypeMappingConfigurationImpl,
    isIrBackend: Boolean
): String {
    val container = if (isIrBackend) getContainer(klass.containingDeclaration) else klass.containingDeclaration

    val name = SpecialNames.safeIdentifier(klass.name).identifier
    if (container is PackageFragmentDescriptor) {
        val fqName = container.fqName
        return if (fqName.isRoot) name else fqName.asString().replace('.', '/') + '/' + name
    }

    val containerClass = container as? ClassDescriptor
        ?: throw IllegalArgumentException("Unexpected container: $container for $klass")

    val containerInternalName =
        typeMappingConfiguration.getPredefinedInternalNameForClass(containerClass) ?: computeInternalName(
            containerClass,
            typeMappingConfiguration,
            isIrBackend
        )
    return "$containerInternalName$$name"
}

private fun getContainer(container: DeclarationDescriptor?): DeclarationDescriptor? =
    container as? ClassDescriptor ?: container as? PackageFragmentDescriptor ?: container?.let { getContainer(it.containingDeclaration) }

fun getRepresentativeUpperBound(descriptor: TypeParameterDescriptor): KotlinType {
    val upperBounds = descriptor.upperBounds
    assert(!upperBounds.isEmpty()) { "Upper bounds should not be empty: $descriptor" }

    return upperBounds.firstOrNull {
        val classDescriptor = it.constructor.declarationDescriptor as? ClassDescriptor ?: return@firstOrNull false
        classDescriptor.kind != ClassKind.INTERFACE && classDescriptor.kind != ClassKind.ANNOTATION_CLASS
    } ?: upperBounds.first()
}

open class JvmDescriptorTypeWriter<T : Any>(private val jvmTypeFactory: JvmTypeFactory<T>) {
    private var jvmCurrentTypeArrayLevel: Int = 0
    protected var jvmCurrentType: T? = null
        private set

    protected fun clearCurrentType() {
        jvmCurrentType = null
        jvmCurrentTypeArrayLevel = 0
    }

    open fun writeArrayType() {
        if (jvmCurrentType == null) {
            ++jvmCurrentTypeArrayLevel
        }
    }

    open fun writeArrayEnd() {
    }

    open fun writeClass(objectType: T) {
        writeJvmTypeAsIs(objectType)
    }

    protected fun writeJvmTypeAsIs(type: T) {
        if (jvmCurrentType == null) {
            jvmCurrentType =
                    if (jvmCurrentTypeArrayLevel > 0) {
                        jvmTypeFactory.createFromString("[".repeat(jvmCurrentTypeArrayLevel) + jvmTypeFactory.toString(type))
                    } else {
                        type
                    }
        }
    }

    open fun writeTypeVariable(name: Name, type: T) {
        writeJvmTypeAsIs(type)
    }
}
