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

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.components.TypeUsage.*
import org.jetbrains.kotlin.load.java.components.*
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.load.java.lazy.*
import org.jetbrains.kotlin.storage.*
import java.util.HashSet
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.resolve.jvm.PLATFORM_TYPES
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeFlexibility.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import kotlin.properties.*

class LazyJavaTypeResolver(
        private val c: LazyJavaResolverContext,
        private val typeParameterResolver: TypeParameterResolver
) {

    public fun transformJavaType(javaType: JavaType, attr: JavaTypeAttributes): JetType {
        return when (javaType) {
            is JavaPrimitiveType -> {
                val canonicalText = javaType.getCanonicalText()
                val jetType = JavaToKotlinClassMap.INSTANCE.mapPrimitiveKotlinClass(canonicalText)
                assert(jetType != null, "Primitive type is not found: " + canonicalText)
                jetType!!
            }
            is JavaClassifierType ->
                if (PLATFORM_TYPES && attr.allowFlexible && attr.howThisTypeIsUsed != SUPERTYPE)
                    FlexibleJavaClassifierTypeCapabilities.create(
                            LazyJavaClassifierType(javaType, attr.toFlexible(FLEXIBLE_LOWER_BOUND)),
                            LazyJavaClassifierType(javaType, attr.toFlexible(FLEXIBLE_UPPER_BOUND))
                    )
                else LazyJavaClassifierType(javaType, attr)
            is JavaArrayType -> transformArrayType(javaType, attr)
            else -> throw UnsupportedOperationException("Unsupported type: " + javaType)
        }
    }

    public fun transformArrayType(arrayType: JavaArrayType, attr: JavaTypeAttributes, isVararg: Boolean = false): JetType {
        return run { (): JetType ->
            val javaComponentType = arrayType.getComponentType()
            if (javaComponentType is JavaPrimitiveType) {
                val jetType = JavaToKotlinClassMap.INSTANCE.mapPrimitiveKotlinClass("[" + javaComponentType.getCanonicalText())
                if (jetType != null) {
                    return@run if (PLATFORM_TYPES && attr.allowFlexible)
                                   FlexibleJavaClassifierTypeCapabilities.create(jetType, TypeUtils.makeNullable(jetType))
                               else TypeUtils.makeNullableAsSpecified(jetType, !attr.isMarkedNotNull)
                }
            }

            val componentType = transformJavaType(javaComponentType, TYPE_ARGUMENT.toAttributes(attr.allowFlexible))

            if (PLATFORM_TYPES && attr.allowFlexible) {
                return@run FlexibleJavaClassifierTypeCapabilities.create(
                        KotlinBuiltIns.getInstance().getArrayType(INVARIANT, componentType),
                        TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(OUT_VARIANCE, componentType)))
            }

            val projectionKind = if (attr.howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT || isVararg) OUT_VARIANCE else INVARIANT
            val result = KotlinBuiltIns.getInstance().getArrayType(projectionKind, componentType)
            return@run TypeUtils.makeNullableAsSpecified(result, !attr.isMarkedNotNull)
        }.replaceAnnotations(attr.annotations)
    }

    fun makeStarProjection(
                typeParameter: TypeParameterDescriptor,
                attr: JavaTypeAttributes
    ): TypeProjection {
        return if (attr.howThisTypeIsUsed == SUPERTYPE)
                   TypeProjectionImpl(typeParameter.starProjectionType())
               else
                   StarProjectionImpl(typeParameter)
    }

    private inner class LazyJavaClassifierType(
            private val javaType: JavaClassifierType,
            private val attr: JavaTypeAttributes
    ) : AbstractLazyType(c.storageManager) {

        private val classifier = c.storageManager.createNullableLazyValue { javaType.getClassifier() }

        override fun computeTypeConstructor(): TypeConstructor {
            val classifier = classifier()
            if (classifier == null) {
                return ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.getPresentableText())
            }
            return when (classifier) {
                is JavaClass -> {
                    val fqName = classifier.getFqName()
                            .sure("Class type should have a FQ name: " + classifier)

                    val javaToKotlinClassMap = JavaToKotlinClassMap.INSTANCE
                    val howThisTypeIsUsedEffectively = when {
                        attr.flexibility == FLEXIBLE_LOWER_BOUND -> MEMBER_SIGNATURE_COVARIANT
                        attr.flexibility == FLEXIBLE_UPPER_BOUND -> MEMBER_SIGNATURE_CONTRAVARIANT

                        // This case has to be checked before isMarkedReadOnly/isMarkedMutable, because those two are slow
                        // not mapped, we don't care about being marked mutable/read-only
                        javaToKotlinClassMap.mapPlatformClass(fqName).isEmpty() -> attr.howThisTypeIsUsed

                        // Read (possibly external) annotations
                        else -> attr.howThisTypeIsUsedAccordingToAnnotations
                    }

                    val classData = javaToKotlinClassMap.mapKotlinClass(fqName, howThisTypeIsUsedEffectively)
                                    ?: c.moduleClassResolver.resolveClass(classifier)

                    classData?.getTypeConstructor()
                        ?: ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.getPresentableText())
                }
                is JavaTypeParameter -> {
                    if (isConstructorTypeParameter()) {
                        getConstructorTypeParameterSubstitute().getConstructor()
                    }
                    else {
                        typeParameterResolver.resolveTypeParameter(classifier)?.getTypeConstructor()
                            ?: ErrorUtils.createErrorTypeConstructor("Unresolved Java type parameter: " + javaType.getPresentableText())
                    }
                }
                else -> throw IllegalStateException("Unknown classifier kind: $classifier")
            }
        }

        private fun isConstructorTypeParameter(): Boolean {
            val classifier = classifier()
            return classifier is JavaTypeParameter && classifier.getOwner() is JavaConstructor
        }

        // We do not memoize the results of this method, because it would consume much memory, and the real gain is little:
        // the case this method accounts for is very rare, not point in optimizing it
        private fun getConstructorTypeParameterSubstitute(): JetType {
            // If a Java-constructor declares its own type parameters, we have no way of directly expressing them in Kotlin,
            // so we replace them by intersections of their upper bounds
            val supertypesJet = HashSet<JetType>()
            for (supertype in (classifier() as JavaTypeParameter).getUpperBounds()) {
                supertypesJet.add(transformJavaType(supertype, UPPER_BOUND.toAttributes()))
            }
            return TypeUtils.intersect(JetTypeChecker.DEFAULT, supertypesJet)
                        ?: ErrorUtils.createErrorType("Can't intersect upper bounds of " + javaType.getPresentableText())
        }

        private fun isRaw(): Boolean {
            if (javaType.isRaw()) return true

            // This option is needed because sometimes we get weird versions of JDK classes in the class path,
            // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
            // their Kotlin analogs, so we treat them as raw to avoid exceptions
            // No type arguments, but some are expected => raw
            return javaType.getTypeArguments().isEmpty() && !getConstructor().getParameters().isEmpty()
        }

        override fun computeArguments(): List<TypeProjection> {
            val typeConstructor = getConstructor()
            val typeParameters = typeConstructor.getParameters()
            if (isRaw()) {
                return typeParameters.map {
                    parameter ->

                    if (attr.howThisTypeIsUsed == UPPER_BOUND) {
                        // not making a star projection because of this case:
                        // Java:
                        // class C<T extends C> {}
                        // The upper bound is raw here, and we can't compute the projection: it would be infinite:
                        // C<*> = C<out C<out C<...>>>
                        // this way we lose some type information, even when the case is not so bad, but it doesn't seem to matter
                        val projectionKind = if (parameter.getVariance() == OUT_VARIANCE) INVARIANT else OUT_VARIANCE
                        TypeProjectionImpl(projectionKind, KotlinBuiltIns.getInstance().getNullableAnyType())
                    }
                    else
                        makeStarProjection(parameter, attr)
                }
            }
            if (isConstructorTypeParameter()) {
                return getConstructorTypeParameterSubstitute().getArguments()
            }

            if (typeParameters.size() != javaType.getTypeArguments().size()) {
                // Most of the time this means there is an error in the Java code
                return typeParameters.map { p -> TypeProjectionImpl(ErrorUtils.createErrorType(p.getName().asString())) }
            }
            var howTheProjectionIsUsed = if (attr.howThisTypeIsUsed == SUPERTYPE) SUPERTYPE_ARGUMENT else TYPE_ARGUMENT
            return javaType.getTypeArguments().withIndices().map {
                javaTypeParameter ->
                val (i, t) = javaTypeParameter
                val parameter = if (i >= typeParameters.size())
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
                    val bound = javaType.getBound()
                    if (bound == null)
                        makeStarProjection(typeParameter, attr)
                    else {
                        var projectionKind = if (javaType.isExtends()) OUT_VARIANCE else IN_VARIANCE
                        if (projectionKind == typeParameter.getVariance()) {
                            projectionKind = Variance.INVARIANT
                        }
                        TypeProjectionImpl(projectionKind, transformJavaType(bound, UPPER_BOUND.toAttributes()))
                    }
                }
                else -> TypeProjectionImpl(INVARIANT, transformJavaType(javaType, attr))
            }
        }

        override fun computeMemberScope(): JetScope {
            val descriptor = getConstructor().getDeclarationDescriptor()!!

            if (descriptor is TypeParameterDescriptor) return descriptor.getDefaultType().getMemberScope()

            return (descriptor as ClassDescriptor).getMemberScope(getArguments())
        }

        private val nullable = c.storageManager.createLazyValue @l {
            (): Boolean ->
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

        override fun getAnnotations() = attr.annotations
    }

    public object FlexibleJavaClassifierTypeCapabilities : FlexibleTypeCapabilities {
        platformStatic fun create(lowerBound: JetType, upperBound: JetType) = DelegatingFlexibleType.create(lowerBound, upperBound, this)

        override val id: String get() = "kotlin.jvm.PlatformType"

        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T? {
            if (capabilityClass.isAssignableFrom(javaClass<Impl>()))
                [suppress("UNCHECKED_CAST")]
                return Impl(flexibility) as T
            else return null
        }


        private class Impl(val flexibility: Flexibility) : CustomTypeVariable, Specificity {

            private val lowerBound: JetType get() = flexibility.lowerBound
            private val upperBound: JetType get() = flexibility.upperBound

            override val isTypeVariable: Boolean = lowerBound.getConstructor() == upperBound.getConstructor()
                                                   && lowerBound.getConstructor().getDeclarationDescriptor() is TypeParameterDescriptor

            override val typeParameterDescriptor: TypeParameterDescriptor? =
                    if (isTypeVariable) lowerBound.getConstructor().getDeclarationDescriptor() as TypeParameterDescriptor else null

            override fun substitutionResult(replacement: JetType): JetType {
                return if (replacement.isFlexible()) replacement
                       else create(replacement, TypeUtils.makeNullable(replacement))
            }

            override fun getSpecificityRelationTo(otherType: JetType): Specificity.Relation {
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

trait JavaTypeAttributes {
    val howThisTypeIsUsed: TypeUsage
    val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
    val isMarkedNotNull: Boolean
    val flexibility: JavaTypeFlexibility
        get() = INFLEXIBLE
    val allowFlexible: Boolean
        get() = true
    val annotations: Annotations
}

enum class JavaTypeFlexibility {
    INFLEXIBLE
    FLEXIBLE_UPPER_BOUND
    FLEXIBLE_LOWER_BOUND
}

class LazyJavaTypeAttributes(
        c: LazyJavaResolverContext,
        val annotationOwner: JavaAnnotationOwner,
        override val howThisTypeIsUsed: TypeUsage,
        override val annotations: Annotations,
        override val allowFlexible: Boolean = true
): JavaTypeAttributes {

    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage by c.storageManager.createLazyValue {
        if (annotations.isMarkedReadOnly() && !annotations.isMarkedMutable())
            TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
        else
            TypeUsage.MEMBER_SIGNATURE_COVARIANT
    }

    override val isMarkedNotNull: Boolean by c.storageManager.createLazyValue { c.hasNotNullAnnotation(annotationOwner) }
}

private fun Annotations.isMarkedReadOnly() = findAnnotation(JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION) != null
private fun Annotations.isMarkedMutable() = findAnnotation(JvmAnnotationNames.JETBRAINS_MUTABLE_ANNOTATION) != null
internal fun Annotations.isMarkedNotNull() = findAnnotation(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION) != null
internal fun Annotations.isMarkedNullable() = findAnnotation(JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION) != null

fun TypeUsage.toAttributes(allowFlexible: Boolean = true) = object : JavaTypeAttributes {
    override val howThisTypeIsUsed: TypeUsage = this@toAttributes
    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
            get() = howThisTypeIsUsed
    override val isMarkedNotNull: Boolean = false
    override val allowFlexible: Boolean = allowFlexible

    override val annotations: Annotations = Annotations.EMPTY
}

fun JavaTypeAttributes.toFlexible(flexibility: JavaTypeFlexibility) =
        object : JavaTypeAttributes by this {
            override val flexibility = flexibility
        }
