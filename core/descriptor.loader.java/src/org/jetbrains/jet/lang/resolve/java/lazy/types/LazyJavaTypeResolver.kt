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

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaType
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage.*
import org.jetbrains.jet.lang.resolve.java.resolver.*
import org.jetbrains.jet.lang.types.Variance.*
import org.jetbrains.jet.lang.types.*
import org.jetbrains.kotlin.util.iif
import org.jetbrains.jet.utils.*
import org.jetbrains.jet.storage.*
import org.jetbrains.kotlin.util.eq
import org.jetbrains.jet.lang.resolve.java.structure.JavaPrimitiveType
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.java.lazy.TypeParameterResolver
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.TypeParameterResolverImpl
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.scopes.JetScope

class LazyJavaTypeResolver(
        private val c: LazyJavaResolverContext,
        private val typeParameterResolver: TypeParameterResolver
) {
    private val NOT_NULL_POSITIONS = setOf(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT, SUPERTYPE)

    public fun transformJavaType(javaType: JavaType, howThisTypeIsUsed: TypeUsage): JetType {
        return when (javaType) {
            is JavaPrimitiveType -> {
                val canonicalText = javaType.getCanonicalText()
                val jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(canonicalText)
                assert(jetType != null, "Primitive type is not found: " + canonicalText)
                return jetType!!
            }
            is JavaClassifierType -> LazyJavaClassifierType(javaType, howThisTypeIsUsed).applyNullablility(howThisTypeIsUsed)
            is JavaArrayType -> transformArrayType(javaType, howThisTypeIsUsed).applyNullablility(howThisTypeIsUsed)
            else -> throw UnsupportedOperationException("Unsupported type: " + javaType)
        }
    }

    public fun transformArrayType(arrayType: JavaArrayType, howThisTypeIsUsed: TypeUsage, isVararg: Boolean = false): JetType {
        val javaComponentType = arrayType.getComponentType()
        if (javaComponentType is JavaPrimitiveType) {
            val jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass("[" + javaComponentType.getCanonicalText())
            if (jetType != null) return jetType
        }

        val projectionKind = if (howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT && !isVararg) OUT_VARIANCE else INVARIANT

        val howArgumentTypeIsUsed = isVararg.iif(MEMBER_SIGNATURE_CONTRAVARIANT, TYPE_ARGUMENT)
        val componentType = transformJavaType(javaComponentType, howArgumentTypeIsUsed)
        return TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(projectionKind, componentType))
    }

    private fun JetType.applyNullablility(howThisTypeIsUsed: TypeUsage): JetType {
        return TypeUtils.makeNullableAsSpecified(this, howThisTypeIsUsed !in NOT_NULL_POSITIONS)
    }

    private fun transformToTypeProjection(
            javaType: JavaType,
            howThisTypeIsUsed: TypeUsage,
            typeConstructorBeingApplied: () -> TypeConstructor,
            typeParameterIndex: Int
    ): TypeProjection {
        return when (javaType) {
            is JavaWildcardType -> {
                val bound = javaType.getBound()
                if (bound == null)
                    LazyStarProjection(c, typeConstructorBeingApplied, typeParameterIndex)
                else {
                    TypeProjectionImpl(javaType.isExtends().iif(OUT_VARIANCE, IN_VARIANCE), transformJavaType(bound, UPPER_BOUND))
                }
            }
            else -> TypeProjectionImpl(INVARIANT, transformJavaType(javaType, howThisTypeIsUsed))
        }
    }

    private class LazyStarProjection(
            c: LazyJavaResolverContext,
            typeConstructor: () -> TypeConstructor,
            typeParameterIndex: Int
    ) : TypeProjectionBase() {
        private val typeParameter by c.storageManager.createLazyValue {typeConstructor().getParameters()[typeParameterIndex]}

        override fun getProjectionKind() = typeParameter.getVariance().eq(OUT_VARIANCE).iif(INVARIANT, OUT_VARIANCE)
        override fun getType() = typeParameter.getUpperBoundsAsType()
    }

    private inner class LazyJavaClassifierType(
            private val javaType: JavaClassifierType,
            private val howThisTypeIsUsed: TypeUsage
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
                    val classData = JavaToKotlinClassMap.getInstance().mapKotlinClass(fqName, howThisTypeIsUsed)
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
            var howTheProjectionIsUsed = howThisTypeIsUsed.eq(SUPERTYPE).iif(SUPERTYPE_ARGUMENT, TYPE_ARGUMENT)
            return javaType.getTypeArguments().withIndices().map {
                p ->
                val (i, t) = p
                transformToTypeProjection(t, howTheProjectionIsUsed, {getConstructor()}, i)
            }.toList()
        }

        override fun computeMemberScope(): JetScope {
            val descriptor = getConstructor().getDeclarationDescriptor()!!

            if (descriptor is TypeParameterDescriptor) return descriptor.getDefaultType().getMemberScope()

            return (descriptor as ClassDescriptor).getMemberScope(getArguments())
        }
    }
}