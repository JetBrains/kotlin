/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.*
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
    fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor)
}

const val NON_EXISTENT_CLASS_NAME = "error/NonExistentClass"

fun <T : Any> mapType(
        kotlinType: KotlinType,
        factory: JvmTypeFactory<T>,
        mode: TypeMappingMode,
        typeMappingConfiguration: TypeMappingConfiguration<T>,
        descriptorTypeWriter: JvmDescriptorTypeWriter<T>?,
        writeGenericType: (KotlinType, T, TypeMappingMode) -> Unit = DO_NOTHING_3
): T {
    mapBuiltInType(kotlinType, factory)?.let { builtInType ->
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
                factory, mode, typeMappingConfiguration, descriptorTypeWriter, writeGenericType)
    }

    val descriptor =
            constructor.declarationDescriptor
            ?: throw UnsupportedOperationException("no descriptor for type constructor of " + kotlinType)

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
            }
            else {
                descriptorTypeWriter?.writeArrayType()

                arrayElementType =
                        mapType(
                                memberType, factory,
                                mode.toGenericArgumentMode(memberProjection.projectionKind),
                                typeMappingConfiguration, descriptorTypeWriter, writeGenericType)

                descriptorTypeWriter?.writeArrayEnd()
            }

            return factory.createFromString("[" + factory.toString(arrayElementType))
        }

        descriptor is ClassDescriptor -> {
            val jvmType =
                    if (mode.isForAnnotationParameter && KotlinBuiltIns.isKClass(descriptor))
                        factory.javaLangClassType
                    else
                        typeMappingConfiguration.getPredefinedTypeForClass(descriptor.original)
                        ?: factory.createObjectType(computeInternalName(descriptor.original))

            writeGenericType(kotlinType, jvmType, mode)

            return jvmType
        }

        descriptor is TypeParameterDescriptor -> {
            val type = mapType(getRepresentativeUpperBound(descriptor),
                            factory, mode, typeMappingConfiguration, writeGenericType = DO_NOTHING_3, descriptorTypeWriter = null)
            descriptorTypeWriter?.writeTypeVariable(descriptor.getName(), type)
            return type
        }

        else -> throw UnsupportedOperationException("Unknown type " + kotlinType)
    }
}


fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
           && descriptor !is PropertyGetterDescriptor
}

private fun <T : Any> mapBuiltInType(
        type: KotlinType,
        typeFactory: JvmTypeFactory<T>
): T? {
    val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null

    val fqName = descriptor.fqNameUnsafe

    val primitiveType = KotlinBuiltIns.getPrimitiveTypeByFqName(fqName)
    if (primitiveType != null) {
        val jvmType = typeFactory.createFromString(JvmPrimitiveType.get(primitiveType).desc)
        val isNullableInJava = TypeUtils.isNullableType(type) || type.hasEnhancedNullability()
        return typeFactory.boxTypeIfNeeded(jvmType, isNullableInJava)
    }

    val arrayElementType = KotlinBuiltIns.getPrimitiveTypeByArrayClassFqName(fqName)
    if (arrayElementType != null) {
        return typeFactory.createFromString("[" + JvmPrimitiveType.get(arrayElementType).desc)
    }

    val classId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqName)
    if (classId != null) {
        return typeFactory.createObjectType(JvmClassName.byClassId(classId).internalName)
    }

    return null
}

internal fun computeInternalName(klass: ClassDescriptor): String {
    val container = klass.containingDeclaration

    val name = SpecialNames.safeIdentifier(klass.name).identifier
    if (container is PackageFragmentDescriptor) {
        val fqName = container.fqName
        return if (fqName.isRoot) name else fqName.asString().replace('.', '/') + '/' + name
    }

    assert(container is ClassDescriptor) { "Unexpected container: $container for $klass" }

    val containerInternalName = computeInternalName(container as ClassDescriptor)
    return if (klass.kind == ClassKind.ENUM_ENTRY) containerInternalName else containerInternalName + "$" + name
}

private fun getRepresentativeUpperBound(descriptor: TypeParameterDescriptor): KotlinType {
    val upperBounds = descriptor.upperBounds
    assert(!upperBounds.isEmpty()) { "Upper bounds should not be empty: " + descriptor }

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


    open public fun writeClass(objectType: T) {
        writeJvmTypeAsIs(objectType)
    }

    protected fun writeJvmTypeAsIs(type: T) {
        if (jvmCurrentType == null) {
            jvmCurrentType = jvmTypeFactory.createFromString("[".repeat(jvmCurrentTypeArrayLevel) + jvmTypeFactory.toString(type))
        }
    }

    open fun writeTypeVariable(name: Name, type: T) {
        writeJvmTypeAsIs(type)
    }
}
