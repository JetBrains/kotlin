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

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.load.java.ANNOTATIONS_COPIED_TO_TYPES
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.components.TypeUsage.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaAnnotations
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.TypeParameterResolver
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeFlexibility.*
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.typeUtil.createProjection
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.kotlin.utils.toReadOnlyList

private val JAVA_LANG_CLASS_FQ_NAME: FqName = FqName("java.lang.Class")

class JavaTypeResolver(
        private val c: LazyJavaResolverContext,
        private val typeParameterResolver: TypeParameterResolver
) {

    fun transformJavaType(javaType: JavaType, attr: JavaTypeAttributes): KotlinType {
        return when (javaType) {
            is JavaPrimitiveType -> {
                val primitiveType = javaType.type
                if (primitiveType != null) c.module.builtIns.getPrimitiveKotlinType(primitiveType)
                else c.module.builtIns.unitType
            }
            is JavaClassifierType -> transformJavaClassifierType(javaType, attr)
            is JavaArrayType -> transformArrayType(javaType, attr)
            // Top level type can be a wildcard only in case of broken Java code, but we should not fail with exceptions in such cases
            is JavaWildcardType -> javaType.bound?.let { transformJavaType(it, attr) } ?: c.module.builtIns.defaultBound
            else -> throw UnsupportedOperationException("Unsupported type: " + javaType)
        }
    }

    fun transformArrayType(arrayType: JavaArrayType, attr: JavaTypeAttributes, isVararg: Boolean = false): KotlinType {
        return run {
            val javaComponentType = arrayType.componentType
            val primitiveType = (javaComponentType as? JavaPrimitiveType)?.type
            if (primitiveType != null) {
                val jetType = c.module.builtIns.getPrimitiveArrayKotlinType(primitiveType)
                return@run if (attr.allowFlexible)
                    KotlinTypeFactory.flexibleType(jetType, jetType.makeNullableAsSpecified(true))
                else jetType.makeNullableAsSpecified(!attr.isMarkedNotNull)
            }

            val componentType = transformJavaType(javaComponentType,
                                                  TYPE_ARGUMENT.toAttributes(attr.allowFlexible, attr.isForAnnotationParameter))

            if (attr.allowFlexible) {
                return@run KotlinTypeFactory.flexibleType(
                        c.module.builtIns.getArrayType(INVARIANT, componentType),
                        c.module.builtIns.getArrayType(OUT_VARIANCE, componentType).makeNullableAsSpecified(true))
            }

            val projectionKind = if (attr.howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT || isVararg) OUT_VARIANCE else INVARIANT
            val result = c.module.builtIns.getArrayType(projectionKind, componentType)
            return@run result.makeNullableAsSpecified(!attr.isMarkedNotNull)
        }.replaceAnnotations(attr.typeAnnotations)
    }

    private fun transformJavaClassifierType(javaType: JavaClassifierType, attr: JavaTypeAttributes): KotlinType {
        fun errorType() = ErrorUtils.createErrorType("Unresolved java class ${javaType.presentableText}")

        val allowFlexible = attr.allowFlexible && attr.howThisTypeIsUsed != SUPERTYPE
        val isRaw = javaType.isRaw
        if (!javaType.isRaw && !allowFlexible) {
            return computeSimpleJavaClassifierType(javaType, attr) ?: errorType()
        }

        fun computeBound(lower: Boolean) = computeSimpleJavaClassifierType(javaType, attr.computeAttributes(allowFlexible, isRaw, forLower = lower))

        val lower = computeBound(lower = true) ?: return errorType()
        val upper = computeBound(lower = false) ?: return errorType()

        return if (javaType.isRaw) {
            RawTypeImpl(lower, upper)
        }
        else {
            KotlinTypeFactory.flexibleType(lower, upper)
        }
    }

    private fun computeSimpleJavaClassifierType(javaType: JavaClassifierType, attr: JavaTypeAttributes): SimpleType? {
        val annotations = CompositeAnnotations(listOf(LazyJavaAnnotations(c, javaType), attr.typeAnnotations))
        val constructor = computeTypeConstructor(javaType, attr) ?: return null
        val arguments = computeArguments(javaType, attr, constructor)
        val isNullable = isNullable(javaType, attr)

        return KotlinTypeFactory.simpleType(annotations, constructor, arguments, isNullable)
    }

    private fun computeTypeConstructor(javaType: JavaClassifierType, attr: JavaTypeAttributes): TypeConstructor? {
        val classifier = javaType.classifier ?: return createNotFoundClass(javaType)
        return when (classifier) {
            is JavaClass -> {
                val fqName = classifier.fqName.sure { "Class type should have a FQ name: $classifier" }

                val classData = mapKotlinClass(javaType, attr, fqName) ?: c.components.moduleClassResolver.resolveClass(classifier)
                classData?.typeConstructor ?: createNotFoundClass(javaType)
            }
            is JavaTypeParameter -> {
                typeParameterResolver.resolveTypeParameter(classifier)?.typeConstructor
            }
            else -> throw IllegalStateException("Unknown classifier kind: $classifier")
        }
    }

    // There's no way to extract precise type information in PSI when the type's classifier cannot be resolved.
    // So we just take the canonical text of the type (which seems to be the only option at the moment), erase all type arguments
    // and treat the resulting qualified name as if it references a simple top-level class.
    // Note that this makes MISSING_DEPENDENCY_CLASS diagnostic messages not as precise as they could be in some corner cases.
    private fun createNotFoundClass(javaType: JavaClassifierType): TypeConstructor {
        val classId = parseCanonicalFqNameIgnoringTypeArguments(javaType.canonicalText)
        return c.components.deserializedDescriptorResolver.components.notFoundClasses.getClass(classId, listOf(0))
    }

    private fun mapKotlinClass(javaType: JavaClassifierType, attr: JavaTypeAttributes, fqName: FqName): ClassDescriptor? {
        if (attr.isForAnnotationParameter && fqName == JAVA_LANG_CLASS_FQ_NAME) {
            return c.components.reflectionTypes.kClass
        }

        val javaToKotlin = JavaToKotlinClassMap.INSTANCE

        val howThisTypeIsUsedEffectively = when {
            attr.flexibility == FLEXIBLE_LOWER_BOUND -> MEMBER_SIGNATURE_COVARIANT
            attr.flexibility == FLEXIBLE_UPPER_BOUND -> MEMBER_SIGNATURE_CONTRAVARIANT

        // This case has to be checked before isMarkedReadOnly/isMarkedMutable, because those two are slow
        // not mapped, we don't care about being marked mutable/read-only
            javaToKotlin.mapPlatformClass(fqName, c.module.builtIns).isEmpty() -> attr.howThisTypeIsUsed

        // Read (possibly external) annotations
            else -> attr.howThisTypeIsUsedAccordingToAnnotations
        }

        val kotlinDescriptor = javaToKotlin.mapJavaToKotlin(fqName, c.module.builtIns) ?: return null

        if (javaToKotlin.isReadOnly(kotlinDescriptor)) {
            if (howThisTypeIsUsedEffectively == MEMBER_SIGNATURE_COVARIANT
                || howThisTypeIsUsedEffectively == SUPERTYPE
                || javaType.argumentsMakeSenseOnlyForMutableContainer(readOnlyContainer = kotlinDescriptor)) {
                return javaToKotlin.convertReadOnlyToMutable(kotlinDescriptor)
            }
        }

        return kotlinDescriptor
    }

    // Returns true for covariant read-only container that has mutable pair with invariant parameter
    // List<in A> does not make sense, but MutableList<in A> does
    // Same for Map<K, in V>
    // But both Iterable<in A>, MutableIterable<in A> don't make sense as they are covariant, so return false
    private fun JavaClassifierType.argumentsMakeSenseOnlyForMutableContainer(
            readOnlyContainer: ClassDescriptor
    ): Boolean {
        fun JavaType?.isSuperWildcard(): Boolean = (this as? JavaWildcardType)?.let { it.bound != null && !it.isExtends } ?: false

        if (!typeArguments.lastOrNull().isSuperWildcard()) return false
        val mutableLastParameterVariance = JavaToKotlinClassMap.INSTANCE.convertReadOnlyToMutable(readOnlyContainer)
                                                   .typeConstructor.parameters.lastOrNull()?.variance ?: return false

        return mutableLastParameterVariance != OUT_VARIANCE
    }

    fun computeArguments(javaType: JavaClassifierType, attr: JavaTypeAttributes, constructor: TypeConstructor): List<TypeProjection> {
        val eraseTypeParameters = run {
            if (attr.rawBound != RawBound.NOT_RAW) return@run true

            // This option is needed because sometimes we get weird versions of JDK classes in the class path,
            // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
            // their Kotlin analogs, so we treat them as raw to avoid exceptions
            javaType.typeArguments.isEmpty() && !constructor.parameters.isEmpty()
        }

        val typeParameters = constructor.parameters
        if (eraseTypeParameters) {
            return typeParameters.map {
                parameter ->
                // Some activity for preventing recursion in cases like `class A<T extends A, F extends T>`
                //
                // When calculating upper bound of some parameter (attr.upperBoundOfTypeParameter),
                // do not try to start upper bound calculation of it again.
                // If we met such recursive dependency it means that upper bound of `attr.upperBoundOfTypeParameter` based effectively
                // on the current class, so we can manually erase default type of current constructor.
                //
                // In example above corner cases are:
                // - Calculating first argument for raw upper bound of T. It depends on T, so we just get A<*, *>
                // - Calculating second argument for raw upper bound of T. It depends on F, that again depends on upper bound of T,
                //   so we get A<*, *>.
                // Summary result for upper bound of T is `A<A<*, *>, A<*, *>>..A<out A<*, *>, out A<*, *>>`
                val erasedUpperBound =
                        parameter.getErasedUpperBound(attr.upperBoundOfTypeParameter) {
                            constructor.declarationDescriptor!!.defaultType.replaceArgumentsWithStarProjections()
                        }

                RawSubstitution.computeProjection(parameter, attr, erasedUpperBound)
            }.toReadOnlyList()
        }

        if (typeParameters.size != javaType.typeArguments.size) {
            // Most of the time this means there is an error in the Java code
            return typeParameters.map { p -> TypeProjectionImpl(ErrorUtils.createErrorType(p.name.asString())) }.toReadOnlyList()
        }
        val howTheProjectionIsUsed = if (attr.howThisTypeIsUsed == SUPERTYPE) SUPERTYPE_ARGUMENT else TYPE_ARGUMENT
        return javaType.typeArguments.withIndex().map {
            indexedArgument ->
            val (i, javaTypeArgument) = indexedArgument

            assert(i < typeParameters.size) {
                "Argument index should be less then type parameters count, but $i > ${typeParameters.size}"
            }

            val parameter = typeParameters[i]
            transformToTypeProjection(javaTypeArgument, howTheProjectionIsUsed.toAttributes(), parameter)
        }.toReadOnlyList()
    }

    private fun transformToTypeProjection(
            javaType: JavaType,
            attr: JavaTypeAttributes,
            typeParameter: TypeParameterDescriptor
    ): TypeProjection {
        return when (javaType) {
            is JavaWildcardType -> {
                val bound = javaType.bound
                val projectionKind = if (javaType.isExtends) OUT_VARIANCE else IN_VARIANCE
                if (bound == null || projectionKind.isConflictingArgumentFor(typeParameter))
                    makeStarProjection(typeParameter, attr)
                else {
                    createProjection(
                            type = transformJavaType(bound, UPPER_BOUND.toAttributes()),
                            projectionKind = projectionKind,
                            typeParameterDescriptor = typeParameter
                    )
                }
            }
            else -> TypeProjectionImpl(INVARIANT, transformJavaType(javaType, attr))
        }
    }

    private fun Variance.isConflictingArgumentFor(typeParameter: TypeParameterDescriptor): Boolean {
        if (typeParameter.variance == INVARIANT) return false
        return this != typeParameter.variance
    }

    private fun isNullable(javaType: JavaClassifierType, attr: JavaTypeAttributes): Boolean {
        if (attr.flexibility == FLEXIBLE_LOWER_BOUND) return false
        if (attr.flexibility == FLEXIBLE_UPPER_BOUND) return true

        return !attr.isMarkedNotNull &&
        // 'L extends List<T>' in Java is a List<T> in Kotlin, not a List<T?>
        // nullability will be taken care of in individual member signatures
        when (javaType.classifier) {
            is JavaTypeParameter -> {
                attr.howThisTypeIsUsed !in setOf(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT, SUPERTYPE)
            }
            is JavaClass,
            null -> attr.howThisTypeIsUsed !in setOf(TYPE_ARGUMENT, SUPERTYPE_ARGUMENT, SUPERTYPE)
            else -> error("Unknown classifier: ${javaType.classifier}")
        }
    }
}

internal fun makeStarProjection(
        typeParameter: TypeParameterDescriptor,
        attr: JavaTypeAttributes
): TypeProjection {
    return if (attr.howThisTypeIsUsed == SUPERTYPE)
        TypeProjectionImpl(typeParameter.starProjectionType())
    else
        StarProjectionImpl(typeParameter)
}

interface JavaTypeAttributes {
    val howThisTypeIsUsed: TypeUsage
    val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
    val isMarkedNotNull: Boolean
    val flexibility: JavaTypeFlexibility
        get() = INFLEXIBLE
    val allowFlexible: Boolean
        get() = true
    val typeAnnotations: Annotations
    val isForAnnotationParameter: Boolean
        get() = false
    // Current type is upper bound of this type parameter
    val upperBoundOfTypeParameter: TypeParameterDescriptor?
        get() = null
    val rawBound: RawBound
        get() = RawBound.NOT_RAW
}

enum class RawBound {
    LOWER,
    UPPER,
    NOT_RAW
}

enum class JavaTypeFlexibility {
    INFLEXIBLE,
    FLEXIBLE_UPPER_BOUND,
    FLEXIBLE_LOWER_BOUND
}

class LazyJavaTypeAttributes(
        override val howThisTypeIsUsed: TypeUsage,
        annotations: Annotations,
        override val allowFlexible: Boolean = true,
        override val isForAnnotationParameter: Boolean = false
): JavaTypeAttributes {
    override val typeAnnotations = FilteredAnnotations(annotations) { it in ANNOTATIONS_COPIED_TO_TYPES }

    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage get() =
            if (hasAnnotation(JETBRAINS_READONLY_ANNOTATION) && !hasAnnotation(JETBRAINS_MUTABLE_ANNOTATION))
                TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
            else
                TypeUsage.MEMBER_SIGNATURE_COVARIANT

    override val isMarkedNotNull: Boolean get() = typeAnnotations.isMarkedNotNull()

    private fun hasAnnotation(fqName: FqName) = typeAnnotations.findAnnotation(fqName) != null
}

fun Annotations.isMarkedNotNull() = findAnnotation(JETBRAINS_NOT_NULL_ANNOTATION) != null
fun Annotations.isMarkedNullable() = findAnnotation(JETBRAINS_NULLABLE_ANNOTATION) != null

fun TypeUsage.toAttributes(
        allowFlexible: Boolean = true,
        isForAnnotationParameter: Boolean = false,
        upperBoundForTypeParameter: TypeParameterDescriptor? = null
) = object : JavaTypeAttributes {
    override val howThisTypeIsUsed: TypeUsage = this@toAttributes
    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
            get() = howThisTypeIsUsed
    override val isMarkedNotNull: Boolean = false
    override val allowFlexible: Boolean = allowFlexible

    override val typeAnnotations: Annotations = Annotations.EMPTY

    override val isForAnnotationParameter: Boolean = isForAnnotationParameter
    override val upperBoundOfTypeParameter: TypeParameterDescriptor? = upperBoundForTypeParameter
}

fun JavaTypeAttributes.computeAttributes(allowFlexible: Boolean, isRaw: Boolean, forLower: Boolean) =
        object : JavaTypeAttributes by this {
            override val flexibility = if (!allowFlexible) INFLEXIBLE else if(forLower) FLEXIBLE_LOWER_BOUND else FLEXIBLE_UPPER_BOUND
            override val rawBound = if (!isRaw) RawBound.NOT_RAW else if(forLower) RawBound.LOWER else RawBound.UPPER
        }


// Definition:
// ErasedUpperBound(T : G<t>) = G<*> // UpperBound(T) is a type G<t> with arguments
// ErasedUpperBound(T : A) = A // UpperBound(T) is a type A without arguments
// ErasedUpperBound(T : F) = UpperBound(F) // UB(T) is another type parameter F
internal fun TypeParameterDescriptor.getErasedUpperBound(
        // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
        // E.g. `class A<T extends A, F extends A>`
        // To prevent recursive calls return defaultValue() instead
        potentiallyRecursiveTypeParameter: TypeParameterDescriptor? = null,
        defaultValue: (() -> KotlinType) = { ErrorUtils.createErrorType("Can't compute erased upper bound of type parameter `$this`") }
): KotlinType {
    if (this === potentiallyRecursiveTypeParameter) return defaultValue()

    val firstUpperBound = upperBounds.first()

    if (firstUpperBound.constructor.declarationDescriptor is ClassDescriptor) {
        return firstUpperBound.replaceArgumentsWithStarProjections()
    }

    val stopAt = potentiallyRecursiveTypeParameter ?: this
    var current = firstUpperBound.constructor.declarationDescriptor as TypeParameterDescriptor

    while (current != stopAt) {
        val nextUpperBound = current.upperBounds.first()
        if (nextUpperBound.constructor.declarationDescriptor is ClassDescriptor) {
            return nextUpperBound.replaceArgumentsWithStarProjections()
        }

        current = nextUpperBound.constructor.declarationDescriptor as TypeParameterDescriptor
    }

    return defaultValue()
}

