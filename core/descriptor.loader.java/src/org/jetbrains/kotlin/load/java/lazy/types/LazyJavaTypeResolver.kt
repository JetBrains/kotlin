/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.sure

private val JAVA_LANG_CLASS_FQ_NAME: FqName = FqName("java.lang.Class")

class LazyJavaTypeResolver(
        private val c: LazyJavaResolverContext,
        private val typeParameterResolver: TypeParameterResolver
) {

    fun transformJavaType(javaType: JavaType, attr: JavaTypeAttributes): KotlinType {
        return when (javaType) {
            is JavaPrimitiveType -> {
                val primitiveType = javaType.type
                if (primitiveType != null) c.module.builtIns.getPrimitiveKotlinType(primitiveType)
                else c.module.builtIns.getUnitType()
            }
            is JavaClassifierType ->
                if (attr.allowFlexible && attr.howThisTypeIsUsed != SUPERTYPE)
                    FlexibleJavaClassifierTypeCapabilities.create(
                            LazyJavaClassifierType(javaType, attr.toFlexible(FLEXIBLE_LOWER_BOUND)),
                            LazyJavaClassifierType(javaType, attr.toFlexible(FLEXIBLE_UPPER_BOUND))
                    )
                else LazyJavaClassifierType(javaType, attr)
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
                    FlexibleJavaClassifierTypeCapabilities.create(jetType, TypeUtils.makeNullable(jetType))
                else TypeUtils.makeNullableAsSpecified(jetType, !attr.isMarkedNotNull)
            }

            val componentType = transformJavaType(javaComponentType,
                                                  TYPE_ARGUMENT.toAttributes(attr.allowFlexible, attr.isForAnnotationParameter))

            if (attr.allowFlexible) {
                return@run FlexibleJavaClassifierTypeCapabilities.create(
                        c.module.builtIns.getArrayType(INVARIANT, componentType),
                        TypeUtils.makeNullable(c.module.builtIns.getArrayType(OUT_VARIANCE, componentType)))
            }

            val projectionKind = if (attr.howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT || isVararg) OUT_VARIANCE else INVARIANT
            val result = c.module.builtIns.getArrayType(projectionKind, componentType)
            return@run TypeUtils.makeNullableAsSpecified(result, !attr.isMarkedNotNull)
        }.replaceAnnotations(attr.typeAnnotations)
    }

    private inner class LazyJavaClassifierType(
            private val javaType: JavaClassifierType,
            private val attr: JavaTypeAttributes
    ) : AbstractLazyType(c.storageManager) {
        private val annotations = CompositeAnnotations(listOf(LazyJavaAnnotations(c, javaType), attr.typeAnnotations))

        private val classifier = c.storageManager.createNullableLazyValue { javaType.getClassifier() }

        override fun computeTypeConstructor(): TypeConstructor {
            val classifier = classifier()
            if (classifier == null) {
                return ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.presentableText)
            }
            return when (classifier) {
                is JavaClass -> {
                    val fqName = classifier.getFqName().sure { "Class type should have a FQ name: $classifier" }

                    val classData = mapKotlinClass(fqName) ?: c.components.moduleClassResolver.resolveClass(classifier)

                    classData?.typeConstructor
                        ?: ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.presentableText)
                }
                is JavaTypeParameter -> {
                    if (isConstructorTypeParameter()) {
                        getConstructorTypeParameterSubstitute().getConstructor()
                    }
                    else {
                        typeParameterResolver.resolveTypeParameter(classifier)?.typeConstructor
                            ?: ErrorUtils.createErrorTypeConstructor("Unresolved Java type parameter: " + javaType.presentableText)
                    }
                }
                else -> throw IllegalStateException("Unknown classifier kind: $classifier")
            }
        }

        private fun mapKotlinClass(fqName: FqName): ClassDescriptor? {
            if (attr.isForAnnotationParameter && fqName == JAVA_LANG_CLASS_FQ_NAME) {
                return c.reflectionTypes.kClass
            }

            val javaToKotlin = JavaToKotlinClassMap.INSTANCE

            val howThisTypeIsUsedEffectively = when {
                attr.flexibility == FLEXIBLE_LOWER_BOUND -> MEMBER_SIGNATURE_COVARIANT
                attr.flexibility == FLEXIBLE_UPPER_BOUND -> MEMBER_SIGNATURE_CONTRAVARIANT

                // This case has to be checked before isMarkedReadOnly/isMarkedMutable, because those two are slow
                // not mapped, we don't care about being marked mutable/read-only
                javaToKotlin.mapPlatformClass(fqName).isEmpty() -> attr.howThisTypeIsUsed

                // Read (possibly external) annotations
                else -> attr.howThisTypeIsUsedAccordingToAnnotations
            }

            val kotlinDescriptor = javaToKotlin.mapJavaToKotlin(fqName) ?: return null

            if (howThisTypeIsUsedEffectively == MEMBER_SIGNATURE_COVARIANT || howThisTypeIsUsedEffectively == SUPERTYPE) {
                if (javaToKotlin.isReadOnly(kotlinDescriptor)) {
                    return javaToKotlin.convertReadOnlyToMutable(kotlinDescriptor)
                }
            }

            return kotlinDescriptor
        }

        private fun isConstructorTypeParameter(): Boolean {
            val classifier = classifier()
            return classifier is JavaTypeParameter && classifier.getOwner() is JavaConstructor
        }

        // We do not memoize the results of this method, because it would consume much memory, and the real gain is little:
        // the case this method accounts for is very rare, no point in optimizing it
        private fun getConstructorTypeParameterSubstitute(): KotlinType {
            // If a Java constructor declares its own type parameters, we have no way of directly expressing them in Kotlin,
            // so we replace each type parameter with its representative upper bound (which in Java is also the first bound)
            val upperBounds = (classifier() as JavaTypeParameter).upperBounds
            if (upperBounds.isEmpty()) {
                return c.module.builtIns.nullableAnyType
            }

            return transformJavaType(upperBounds.first(), UPPER_BOUND.toAttributes())
        }

        private fun isRaw(): Boolean {
            if (javaType.isRaw) return true

            // This option is needed because sometimes we get weird versions of JDK classes in the class path,
            // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
            // their Kotlin analogs, so we treat them as raw to avoid exceptions
            // No type arguments, but some are expected => raw
            return javaType.typeArguments.isEmpty() && !getConstructor().parameters.isEmpty()
        }

        override fun computeArguments(): List<TypeProjection> {
            val typeConstructor = getConstructor()
            val typeParameters = typeConstructor.parameters
            if (isRaw()) {
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
                }
            }
            if (isConstructorTypeParameter()) {
                return getConstructorTypeParameterSubstitute().getArguments()
            }

            if (typeParameters.size != javaType.typeArguments.size) {
                // Most of the time this means there is an error in the Java code
                return typeParameters.map { p -> TypeProjectionImpl(ErrorUtils.createErrorType(p.name.asString())) }
            }
            var howTheProjectionIsUsed = if (attr.howThisTypeIsUsed == SUPERTYPE) SUPERTYPE_ARGUMENT else TYPE_ARGUMENT
            return javaType.typeArguments.withIndex().map {
                javaTypeParameter ->
                val (i, t) = javaTypeParameter
                val parameter = if (i >= typeParameters.size)
                                    ErrorUtils.createErrorTypeParameter(i, "#$i for ${typeConstructor}")
                                else typeParameters[i]
                transformToTypeProjection(t, howTheProjectionIsUsed.toAttributes(), parameter)
            }.toList()
        }

        private fun transformToTypeProjection(
                javaType: JavaType,
                attr: JavaTypeAttributes,
                typeParameter: TypeParameterDescriptor
        ): TypeProjection {
            return when (javaType) {
                is JavaWildcardType -> {
                    val bound = javaType.bound
                    if (bound == null)
                        makeStarProjection(typeParameter, attr)
                    else {
                        createProjection(
                                type = transformJavaType(bound, UPPER_BOUND.toAttributes()),
                                projectionKind = if (javaType.isExtends()) OUT_VARIANCE else IN_VARIANCE,
                                typeParameterDescriptor = typeParameter
                        )
                    }
                }
                else -> TypeProjectionImpl(INVARIANT, transformJavaType(javaType, attr))
            }
        }

        override fun getCapabilities(): TypeCapabilities = if (isRaw()) RawTypeCapabilities else TypeCapabilities.NONE

        private val nullable = c.storageManager.createLazyValue l@ {
            when (attr.flexibility) {
                FLEXIBLE_LOWER_BOUND -> return@l false
                FLEXIBLE_UPPER_BOUND -> return@l true
            }
            !attr.isMarkedNotNull &&
            // 'L extends List<T>' in Java is a List<T> in Kotlin, not a List<T?>
            // nullability will be taken care of in individual member signatures
            when (classifier()) {
                is JavaTypeParameter -> {
                    if (isConstructorTypeParameter())
                        getConstructorTypeParameterSubstitute().isMarkedNullable()
                    else
                        attr.howThisTypeIsUsed !in setOf(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT, SUPERTYPE)
                }
                is JavaClass,
                null -> attr.howThisTypeIsUsed !in setOf(TYPE_ARGUMENT, SUPERTYPE_ARGUMENT, SUPERTYPE)
                else -> error("Unknown classifier: ${classifier()}")
            }
        }

        override fun isMarkedNullable(): Boolean = nullable()

        override fun getAnnotations() = annotations
    }

    object FlexibleJavaClassifierTypeCapabilities : FlexibleTypeCapabilities {
        @JvmStatic
        fun create(lowerBound: KotlinType, upperBound: KotlinType) = DelegatingFlexibleType.create(lowerBound, upperBound, this)

        override val id: String get() = "kotlin.jvm.PlatformType"

        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: KotlinType, flexibility: Flexibility): T? {
            @Suppress("UNCHECKED_CAST")
            return when (capabilityClass) {
                CustomTypeVariable::class.java, Specificity::class.java -> Impl(flexibility) as T
                else -> null
            }
        }


        private class Impl(val flexibility: Flexibility) : CustomTypeVariable, Specificity {

            private val lowerBound: KotlinType get() = flexibility.lowerBound
            private val upperBound: KotlinType get() = flexibility.upperBound

            override val isTypeVariable: Boolean = lowerBound.getConstructor() == upperBound.getConstructor()
                                                   && lowerBound.getConstructor().getDeclarationDescriptor() is TypeParameterDescriptor

            override val typeParameterDescriptor: TypeParameterDescriptor? =
                    if (isTypeVariable) lowerBound.getConstructor().getDeclarationDescriptor() as TypeParameterDescriptor else null

            override fun substitutionResult(replacement: KotlinType): KotlinType {
                return if (replacement.isFlexible()) replacement
                       else create(replacement, TypeUtils.makeNullable(replacement))
            }

            override fun getSpecificityRelationTo(otherType: KotlinType): Specificity.Relation {
                // For primitive types we have to take care of the case when there are two overloaded methods like
                //    foo(int) and foo(Integer)
                // if we do not discriminate one of them, any call to foo(kotlin.Int) will result in overload resolution ambiguity
                // so, for such cases, we discriminate Integer in favour of int
                if (!KotlinBuiltIns.isPrimitiveType(otherType) || !KotlinBuiltIns.isPrimitiveType(lowerBound)) {
                    return Specificity.Relation.DONT_KNOW
                }
                // Int! >< Int?
                if (otherType.isFlexible()) return Specificity.Relation.DONT_KNOW
                // Int? >< Int!
                if (otherType.isMarkedNullable()) return Specificity.Relation.DONT_KNOW
                // Int! lessSpecific Int
                return Specificity.Relation.LESS_SPECIFIC
            }
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

fun JavaTypeAttributes.toFlexible(flexibility: JavaTypeFlexibility) =
        object : JavaTypeAttributes by this {
            override val flexibility = flexibility
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

private fun KotlinType.replaceArgumentsWithStarProjections(): KotlinType {
    if (constructor.parameters.isEmpty() || constructor.declarationDescriptor == null) return this

    // We could just create JetTypeImpl with current type constructor and star projections,
    // but we want to preserve flexibility of type, and that it what TypeSubstitutor does
    return TypeSubstitutor.create(ConstantStarSubstitution).substitute(this, Variance.INVARIANT)!!
}

private object ConstantStarSubstitution : TypeSubstitution() {
    override fun get(key: KotlinType): TypeProjection? {
        // Let substitutor deal with flexibility
        if (key.isFlexible()) return null

        val newProjections = key.constructor.parameters.map(::StarProjectionImpl)

        val substitution = TypeConstructorSubstitution.create(key.constructor, newProjections)

        return TypeProjectionImpl(
                TypeSubstitutor.create(substitution).substitute(key.constructor.declarationDescriptor!!.defaultType, Variance.INVARIANT)!!
        )
    }

    override fun isEmpty() = false
}
