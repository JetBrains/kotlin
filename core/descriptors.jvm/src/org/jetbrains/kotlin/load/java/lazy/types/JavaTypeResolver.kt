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

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.components.TypeUsage.COMMON
import org.jetbrains.kotlin.load.java.components.TypeUsage.SUPERTYPE
import org.jetbrains.kotlin.load.java.lazy.LazyJavaAnnotations
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.TypeParameterResolver
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeFlexibility.*
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.sure

private val JAVA_LANG_CLASS_FQ_NAME: FqName = FqName("java.lang.Class")

class JavaTypeResolver(
    private val c: LazyJavaResolverContext,
    private val typeParameterResolver: TypeParameterResolver
) {
    private val typeParameterUpperBoundEraser = TypeParameterUpperBoundEraser()
    private val rawSubstitution = RawSubstitution(typeParameterUpperBoundEraser)

    fun transformJavaType(javaType: JavaType?, attr: JavaTypeAttributes): KotlinType {
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
            null -> c.module.builtIns.defaultBound
            else -> throw UnsupportedOperationException("Unsupported type: $javaType")
        }
    }

    fun transformArrayType(arrayType: JavaArrayType, attr: JavaTypeAttributes, isVararg: Boolean = false): KotlinType {
        val javaComponentType = arrayType.componentType
        val primitiveType = (javaComponentType as? JavaPrimitiveType)?.type
        val annotations = LazyJavaAnnotations(c, arrayType, areAnnotationsFreshlySupported = true)

        if (primitiveType != null) {
            val jetType = c.module.builtIns.getPrimitiveArrayKotlinType(primitiveType)

            jetType.replaceAnnotations(Annotations.create(annotations + jetType.annotations))

            return if (attr.isForAnnotationParameter)
                jetType
            else KotlinTypeFactory.flexibleType(jetType, jetType.makeNullableAsSpecified(true))
        }

        val componentType = transformJavaType(
            javaComponentType,
            COMMON.toAttributes(attr.isForAnnotationParameter)
        )

        if (attr.isForAnnotationParameter) {
            val projectionKind = if (isVararg) OUT_VARIANCE else INVARIANT
            return c.module.builtIns.getArrayType(projectionKind, componentType, annotations)
        }

        return KotlinTypeFactory.flexibleType(
            c.module.builtIns.getArrayType(INVARIANT, componentType, annotations),
            c.module.builtIns.getArrayType(OUT_VARIANCE, componentType, annotations).makeNullableAsSpecified(true)
        )
    }

    private fun transformJavaClassifierType(javaType: JavaClassifierType, attr: JavaTypeAttributes): KotlinType {
        fun errorType() = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_JAVA_CLASS, javaType.presentableText)

        val useFlexible = !attr.isForAnnotationParameter && attr.howThisTypeIsUsed != SUPERTYPE
        val isRaw = javaType.isRaw
        if (!isRaw && !useFlexible) {
            return computeSimpleJavaClassifierType(javaType, attr, null) ?: errorType()
        }

        val lower =
            computeSimpleJavaClassifierType(javaType, attr.withFlexibility(FLEXIBLE_LOWER_BOUND), lowerResult = null)
                ?: return errorType()
        val upper =
            computeSimpleJavaClassifierType(javaType, attr.withFlexibility(FLEXIBLE_UPPER_BOUND), lowerResult = lower)
                ?: return errorType()

        return if (isRaw) {
            RawTypeImpl(lower, upper)
        } else {
            KotlinTypeFactory.flexibleType(lower, upper)
        }
    }

    private fun computeSimpleJavaClassifierType(
        javaType: JavaClassifierType,
        attr: JavaTypeAttributes,
        lowerResult: SimpleType?
    ): SimpleType? {
        val attributes =
            lowerResult?.attributes ?: LazyJavaAnnotations(c, javaType).toDefaultAttributes()
        val constructor = computeTypeConstructor(javaType, attr) ?: return null
        val isNullable = attr.isNullable()

        if (lowerResult?.constructor == constructor && !javaType.isRaw && isNullable) {
            return lowerResult.makeNullableAsSpecified(true)
        }

        val arguments = computeArguments(javaType, attr, constructor)

        return KotlinTypeFactory.simpleType(attributes, constructor, arguments, isNullable)
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
        val classId = ClassId.topLevel(FqName(javaType.classifierQualifiedName))
        return c.components.deserializedDescriptorResolver.components.notFoundClasses.getClass(classId, listOf(0)).typeConstructor
    }

    private fun mapKotlinClass(javaType: JavaClassifierType, attr: JavaTypeAttributes, fqName: FqName): ClassDescriptor? {
        if (attr.isForAnnotationParameter && fqName == JAVA_LANG_CLASS_FQ_NAME) {
            return c.components.reflectionTypes.kClass
        }

        val javaToKotlin = JavaToKotlinClassMapper

        val kotlinDescriptor = javaToKotlin.mapJavaToKotlin(fqName, c.module.builtIns) ?: return null

        if (javaToKotlin.isReadOnly(kotlinDescriptor)) {
            if (attr.flexibility == FLEXIBLE_LOWER_BOUND ||
                attr.howThisTypeIsUsed == SUPERTYPE ||
                javaType.argumentsMakeSenseOnlyForMutableContainer(readOnlyContainer = kotlinDescriptor)
            ) {
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
        if (!typeArguments.lastOrNull().isSuperWildcard()) return false
        val mutableLastParameterVariance = JavaToKotlinClassMapper.convertReadOnlyToMutable(readOnlyContainer)
            .typeConstructor.parameters.lastOrNull()?.variance ?: return false

        return mutableLastParameterVariance != OUT_VARIANCE
    }

    private fun computeRawTypeArguments(
        javaType: JavaClassifierType,
        typeParameters: List<TypeParameterDescriptor>,
        constructor: TypeConstructor,
        attr: JavaTypeAttributes
    ) = typeParameters.map { parameter ->
        /*
         * We shouldn't erase recursive type parameters to avoid creating types unsatisfying upper bounds.
         * E.g. if we got erased raw type of `class Foo<T: Foo<T>> {}` we'd create Foo<(raw) Foo<*>!>!,
         * but it's wrong because Foo<*> isn't subtype of Foo<Foo<*>> in accordance with declared upper bound of Foo.
         * So we should create Foo<*> in this case (CapturedType(*) is really subtype of Foo<CapturedType(*)>).
         */
        if (hasTypeParameterRecursiveBounds(parameter, selfConstructor = null, attr.visitedTypeParameters))
            return@map makeStarProjection(parameter, attr)

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
        val erasedUpperBound = LazyWrappedType(c.storageManager) {
            typeParameterUpperBoundEraser.getErasedUpperBound(
                parameter,
                javaType.isRaw,
                attr.withDefaultType(constructor.declarationDescriptor?.defaultType)
            )
        }

        rawSubstitution.computeProjection(
            parameter,
            // if erasure happens due to invalid arguments number, use star projections instead
            if (javaType.isRaw) attr else attr.withFlexibility(INFLEXIBLE),
            erasedUpperBound
        )
    }

    private fun computeArguments(
        javaType: JavaClassifierType,
        attr: JavaTypeAttributes,
        constructor: TypeConstructor
    ): List<TypeProjection> {
        val isRaw = javaType.isRaw
        val eraseTypeParameters =
            isRaw ||
                    // This option is needed because sometimes we get weird versions of JDK classes in the class path,
                    // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
                    // their Kotlin analogs, so we treat them as raw to avoid exceptions
                    (javaType.typeArguments.isEmpty() && constructor.parameters.isNotEmpty())

        val typeParameters = constructor.parameters
        if (eraseTypeParameters) {
            return computeRawTypeArguments(javaType, typeParameters, constructor, attr)
        }

        if (typeParameters.size != javaType.typeArguments.size) {
            // Most of the time this means there is an error in the Java code
            return typeParameters.map { p ->
                TypeProjectionImpl(ErrorUtils.createErrorType(ErrorTypeKind.MISSED_TYPE_ARGUMENT_FOR_TYPE_PARAMETER, p.name.asString()))
            }.toList()
        }
        return javaType.typeArguments.withIndex().map { indexedArgument ->
            val (i, javaTypeArgument) = indexedArgument

            assert(i < typeParameters.size) {
                "Argument index should be less then type parameters count, but $i > ${typeParameters.size}"
            }

            val parameter = typeParameters[i]
            transformToTypeProjection(javaTypeArgument, COMMON.toAttributes(), parameter)
        }.toList()
    }

    private fun transformToTypeProjection(
        javaType: JavaType?,
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
                        type = transformJavaType(bound, COMMON.toAttributes()),
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

    private fun JavaTypeAttributes.isNullable(): Boolean {
        if (flexibility == FLEXIBLE_LOWER_BOUND) return false

        // even if flexibility is FLEXIBLE_UPPER_BOUND it's still can be not nullable for supertypes and annotation parameters
        return !isForAnnotationParameter && howThisTypeIsUsed != SUPERTYPE
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

data class JavaTypeAttributes(
    val howThisTypeIsUsed: TypeUsage,
    val flexibility: JavaTypeFlexibility = INFLEXIBLE,
    val isForAnnotationParameter: Boolean = false,
    // we use it to prevent happening a recursion while compute type parameter's upper bounds
    val visitedTypeParameters: Set<TypeParameterDescriptor>? = null,
    val defaultType: SimpleType? = null
) {
    fun withFlexibility(flexibility: JavaTypeFlexibility) = copy(flexibility = flexibility)
    fun withDefaultType(type: SimpleType?) = copy(defaultType = type)
    fun withNewVisitedTypeParameter(typeParameter: TypeParameterDescriptor) =
        copy(visitedTypeParameters = if (visitedTypeParameters != null) visitedTypeParameters + typeParameter else setOf(typeParameter))
}

enum class JavaTypeFlexibility {
    INFLEXIBLE,
    FLEXIBLE_UPPER_BOUND,
    FLEXIBLE_LOWER_BOUND
}

fun TypeUsage.toAttributes(
    isForAnnotationParameter: Boolean = false,
    upperBoundForTypeParameter: TypeParameterDescriptor? = null
) = JavaTypeAttributes(
    this,
    isForAnnotationParameter = isForAnnotationParameter,
    visitedTypeParameters = upperBoundForTypeParameter?.let(::setOf)
)
