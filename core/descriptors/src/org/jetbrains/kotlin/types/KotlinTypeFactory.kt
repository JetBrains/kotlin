/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.getRefinedMemberScopeIfPossible
import org.jetbrains.kotlin.descriptors.impl.getRefinedUnsubstitutedMemberScopeIfPossible
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getKotlinTypeRefiner
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

typealias RefinedTypeFactory = (KotlinTypeRefiner) -> SimpleType?

object KotlinTypeFactory {
    val EMPTY_REFINED_TYPE_FACTORY: RefinedTypeFactory = { _ -> null }

    @UseExperimental(TypeRefinement::class)
    private fun computeMemberScope(
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        kotlinTypeRefiner: KotlinTypeRefiner? = null
    ): MemberScope {
        val basicDescriptor = constructor.declarationDescriptor
        val classId = basicDescriptor.safeAs<ClassifierDescriptorWithTypeParameters>()?.classId
        val descriptor =
            if (classId != null)
                kotlinTypeRefiner?.findClassAcrossModuleDependencies(classId) ?: basicDescriptor
            else basicDescriptor

        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor -> {
                val refinedConstructor =
                    if (descriptor != basicDescriptor)
                        descriptor.typeConstructor
                    else
                        constructor

                val refinerToUse = kotlinTypeRefiner ?: descriptor.module.getKotlinTypeRefiner()
                if (arguments.isEmpty())
                    descriptor.getRefinedUnsubstitutedMemberScopeIfPossible(refinerToUse)
                else
                    // REVIEW
                    descriptor.getRefinedMemberScopeIfPossible(
                        TypeConstructorSubstitution.create(refinedConstructor, arguments),
                        refinerToUse
                    )
            }
            is TypeAliasDescriptor -> ErrorUtils.createErrorScope("Scope for abbreviation: ${descriptor.name}", true)
            else -> throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
        }
    }

    @JvmStatic
    fun simpleType(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType {
        if (annotations.isEmpty() && arguments.isEmpty() && !nullable && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            annotations, constructor, arguments, nullable,
            computeMemberScope(constructor, arguments)
        ) { moduleDescriptor ->
            computeMemberScope(constructor, arguments, moduleDescriptor)
        }
    }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, { memberScope })
            .let {
                if (annotations.isEmpty())
                    it
                else
                    AnnotatedSimpleType(it, annotations)
            }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope,
        scopeFactory: (KotlinTypeRefiner) -> MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, scopeFactory)
            .let {
                if (annotations.isEmpty())
                    it
                else
                    AnnotatedSimpleType(it, annotations)
            }

    @JvmStatic
    fun simpleNotNullType(
        annotations: Annotations,
        descriptor: ClassDescriptor,
        arguments: List<TypeProjection>
    ): SimpleType = simpleType(annotations, descriptor.typeConstructor, arguments, nullable = false)

    @JvmStatic
    fun simpleType(
        baseType: SimpleType,
        annotations: Annotations = baseType.annotations,
        constructor: TypeConstructor = baseType.constructor,
        arguments: List<TypeProjection> = baseType.arguments,
        nullable: Boolean = baseType.isMarkedNullable
    ): SimpleType = simpleType(annotations, constructor, arguments, nullable)

    @JvmStatic
    fun flexibleType(lowerBound: SimpleType, upperBound: SimpleType): UnwrappedType {
        if (lowerBound == upperBound) return lowerBound
        return FlexibleTypeImpl(lowerBound, upperBound)
    }

    @JvmStatic
    fun integerLiteralType(
        annotations: Annotations,
        constructor: IntegerLiteralTypeConstructor,
        nullable: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        annotations,
        constructor,
        emptyList(),
        nullable,
        ErrorUtils.createErrorScope("Scope for integer literal type", true)
    )
}

private class SimpleTypeImpl(
    override val constructor: TypeConstructor,
    override val arguments: List<TypeProjection>,
    override val isMarkedNullable: Boolean,
    override val memberScope: MemberScope,
    private val scopeFactory: (KotlinTypeRefiner) -> MemberScope
) : SimpleType() {
    @TypeRefinement
    override val hasNotTrivialRefinementFactory: Boolean get() = true

    override val annotations: Annotations get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations) =
        if (newAnnotations.isEmpty())
            this
        else
            AnnotatedSimpleType(this, newAnnotations)

    override fun makeNullableAsSpecified(newNullability: Boolean) = when {
        newNullability == isMarkedNullable -> this
        newNullability -> NullableSimpleType(this)
        else -> NotNullSimpleType(this)
    }

    init {
        if (memberScope is ErrorUtils.ErrorScope) {
            throw IllegalStateException("SimpleTypeImpl should not be created for error type: $memberScope\n$constructor")
        }
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType {
        if (constructor.declarationDescriptor?.module?.getKotlinTypeRefiner() === kotlinTypeRefiner) return this

        return SimpleTypeImpl(
            constructor.refine(kotlinTypeRefiner) ?: constructor,
            arguments, isMarkedNullable, scopeFactory(kotlinTypeRefiner), scopeFactory
        )
    }
}

abstract class DelegatingSimpleTypeImpl(override val delegate: SimpleType) : DelegatingSimpleType() {
    override fun replaceAnnotations(newAnnotations: Annotations) =
        if (newAnnotations !== annotations)
            AnnotatedSimpleType(this, newAnnotations)
        else
            this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        if (newNullability == isMarkedNullable) return this
        return delegate.makeNullableAsSpecified(newNullability).replaceAnnotations(annotations)
    }
}

private class AnnotatedSimpleType(
    delegate: SimpleType,
    override val annotations: Annotations
) : DelegatingSimpleTypeImpl(delegate) {
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = AnnotatedSimpleType(delegate, annotations)
}

private class NullableSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedNullable: Boolean
        get() = true

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NullableSimpleType(delegate)
}

private class NotNullSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedNullable: Boolean
        get() = false

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NotNullSimpleType(delegate)
}
