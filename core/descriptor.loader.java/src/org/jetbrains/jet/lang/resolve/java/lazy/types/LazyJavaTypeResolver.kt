/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.types

import org.jetbrains.jet.lang.resolve.java.structure.JavaType
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage.*
import org.jetbrains.jet.lang.resolve.java.resolver.*
import org.jetbrains.jet.lang.types.Variance.*
import org.jetbrains.jet.lang.types.*
import org.jetbrains.kotlin.util.iif
import org.jetbrains.kotlin.util.eq
import org.jetbrains.jet.lang.resolve.java.structure.JavaPrimitiveType
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationOwner
import org.jetbrains.jet.lang.resolve.java.lazy.*
import org.jetbrains.jet.storage.*

class LazyJavaTypeResolver(
        private val c: LazyJavaResolverContext,
        private val typeParameterResolver: TypeParameterResolver
) {
    private val NOT_NULL_POSITIONS = setOf(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT, SUPERTYPE)

    public fun transformJavaType(javaType: JavaType, attr: JavaTypeAttributes): JetType {
        return when (javaType) {
            is JavaPrimitiveType -> {
                val canonicalText = javaType.getCanonicalText()
                val jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(canonicalText)
                assert(jetType != null, "Primitive type is not found: " + canonicalText)
                return jetType!!
            }
            is JavaClassifierType -> LazyJavaClassifierType(javaType, attr).applyNullablility(attr)
            is JavaArrayType -> transformArrayType(javaType, attr).applyNullablility(attr)
            else -> throw UnsupportedOperationException("Unsupported type: " + javaType)
        }
    }

    public fun transformArrayType(arrayType: JavaArrayType, attr: JavaTypeAttributes, isVararg: Boolean = false): JetType {
        val javaComponentType = arrayType.getComponentType()
        if (javaComponentType is JavaPrimitiveType) {
            val jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass("[" + javaComponentType.getCanonicalText())
            if (jetType != null) return jetType
        }

        val projectionKind = if (attr.howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT && !isVararg) OUT_VARIANCE else INVARIANT

        val howArgumentTypeIsUsed = isVararg.iif(MEMBER_SIGNATURE_CONTRAVARIANT, TYPE_ARGUMENT)
        val componentType = transformJavaType(javaComponentType, howArgumentTypeIsUsed.toAttributes())
        return TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(projectionKind, componentType))
    }

    private fun JetType.applyNullablility(attr: JavaTypeAttributes): JetType {
        if (attr.howThisTypeIsUsed in NOT_NULL_POSITIONS) {
            return TypeUtils.makeNotNullable(this)
        }
        return this
    }

    private fun transformToTypeProjection(
            javaType: JavaType,
            attr: JavaTypeAttributes,
            typeConstructorBeingApplied: () -> TypeConstructor,
            typeParameterIndex: Int
    ): TypeProjection {
        return when (javaType) {
            is JavaWildcardType -> {
                val bound = javaType.getBound()
                if (bound == null)
                    LazyStarProjection(c, typeConstructorBeingApplied, typeParameterIndex)
                else {
                    TypeProjectionImpl(javaType.isExtends().iif(OUT_VARIANCE, IN_VARIANCE), transformJavaType(bound, UPPER_BOUND.toAttributes()))
                }
            }
            else -> TypeProjectionImpl(INVARIANT, transformJavaType(javaType, attr))
        }
    }

    private class LazyStarProjection(
            c: LazyJavaResolverContext,
            typeConstructor: () -> TypeConstructor,
            typeParameterIndex: Int
    ) : TypeProjectionBase() {
        private val typeParameter by c.storageManager.createLazyValue {
            val typeConstructor = typeConstructor()
            val parameters = typeConstructor.getParameters()
            if (typeParameterIndex >= parameters.size)
                ErrorUtils.createErrorTypeParameter("#$typeParameterIndex for ${typeConstructor}")
            else parameters[typeParameterIndex]
        }

        override fun getProjectionKind() = typeParameter.getVariance().eq(OUT_VARIANCE).iif(INVARIANT, OUT_VARIANCE)
        override fun getType() = typeParameter.getUpperBoundsAsType()
    }

    private inner class LazyJavaClassifierType(
            private val javaType: JavaClassifierType,
            private val attr: JavaTypeAttributes
    ) : LazyType(c.storageManager) {


        override fun computeTypeConstructor(): TypeConstructor {
            val classifier = javaType.getClassifier()
            if (classifier == null) {
                return ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.getPresentableText())
            }
            return when (classifier) {
                is JavaClass -> {
                    val fqName = classifier.getFqName()
                            .sure("Class type should have a FQ name: " + classifier)

                    val javaToKotlinClassMap = JavaToKotlinClassMap.getInstance()
                    val howThisTypeIsUsedEffectively = when {
                        // This case has to be checked before isMarkedReadOnly/isMarkedMutable, because those two are slow
                        // not mapped, we don't care about being marked mutable/read-only
                        javaToKotlinClassMap.mapPlatformClass(fqName).isEmpty() -> attr.howThisTypeIsUsed

                        // Read (possibly external) annotations
                        else -> attr.howThisTypeIsUsedAccordingToAnnotations
                    }

                    val classData = javaToKotlinClassMap.mapKotlinClass(fqName, howThisTypeIsUsedEffectively)
                                    ?: c.javaClassResolver.resolveClass(classifier)

                    classData?.getTypeConstructor()
                        ?: ErrorUtils.createErrorTypeConstructor("Unresolved java classifier: " + javaType.getPresentableText())
                }
                is JavaTypeParameter -> {
                    typeParameterResolver.resolveTypeParameter(classifier)?.getTypeConstructor()
                        ?: ErrorUtils.createErrorTypeConstructor("Unresolved Java type parameter: " + javaType.getPresentableText())
                }
                else -> throw IllegalStateException("Unknown classifier kind: $classifier")
            }
        }

        override fun computeArguments(): List<TypeProjection> {
            var howTheProjectionIsUsed = attr.howThisTypeIsUsed.eq(SUPERTYPE).iif(SUPERTYPE_ARGUMENT, TYPE_ARGUMENT)
            return javaType.getTypeArguments().withIndices().map {
                p ->
                val (i, t) = p
                transformToTypeProjection(t, howTheProjectionIsUsed.toAttributes(), {getConstructor()}, i)
            }.toList()
        }

        override fun computeMemberScope(): JetScope {
            val descriptor = getConstructor().getDeclarationDescriptor()!!

            if (descriptor is TypeParameterDescriptor) return descriptor.getDefaultType().getMemberScope()

            return (descriptor as ClassDescriptor).getMemberScope(getArguments())
        }

        private val _nullable = c.storageManager.createLazyValue { !attr.isMarkedNotNull }
        override fun isNullable(): Boolean = _nullable()
    }
}

trait JavaTypeAttributes {
    val howThisTypeIsUsed: TypeUsage
    val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
    val isMarkedNotNull: Boolean
}

class LazyJavaTypeAttributes(
        c: LazyJavaResolverContext,
        val annotationOwner: JavaAnnotationOwner,
        override val howThisTypeIsUsed: TypeUsage,
        computeHowThisTypeIsUsedAccrodingToAnnotations: () -> TypeUsage
): JavaTypeAttributes {

    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage by c.storageManager.createLazyValue(
        computeHowThisTypeIsUsedAccrodingToAnnotations
    )

    override val isMarkedNotNull: Boolean by c.storageManager.createLazyValue { annotationOwner.hasNotNullAnnotation() }
}

fun TypeUsage.toAttributes() = object : JavaTypeAttributes {
    override val howThisTypeIsUsed: TypeUsage = this@toAttributes
    override val howThisTypeIsUsedAccordingToAnnotations: TypeUsage
            get() = howThisTypeIsUsed
    override val isMarkedNotNull: Boolean = false
}