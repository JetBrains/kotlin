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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.getRefinedMemberScopeIfPossible
import org.jetbrains.kotlin.descriptors.impl.getRefinedUnsubstitutedMemberScopeIfPossible
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.getKotlinTypeRefiner
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

typealias RefinedTypeFactory = (KotlinTypeRefiner) -> SimpleType?

object KotlinTypeFactory {
    val EMPTY_REFINED_TYPE_FACTORY: RefinedTypeFactory = { _ -> null }

    @OptIn(TypeRefinement::class)
    private fun computeMemberScope(
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        kotlinTypeRefiner: KotlinTypeRefiner? = null
    ): MemberScope {
        val descriptor = constructor.declarationDescriptor
        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor -> {
                val refinerToUse = kotlinTypeRefiner ?: descriptor.module.getKotlinTypeRefiner()
                if (arguments.isEmpty())
                    descriptor.getRefinedUnsubstitutedMemberScopeIfPossible(refinerToUse)
                else
                    // REVIEW
                    descriptor.getRefinedMemberScopeIfPossible(
                        TypeConstructorSubstitution.create(constructor, arguments),
                        refinerToUse
                    )
            }
            is TypeAliasDescriptor -> ErrorUtils.createErrorScope("Scope for abbreviation: ${descriptor.name}", true)
            else -> {
                if (constructor is IntersectionTypeConstructor) {
                    return constructor.createScopeForKotlinType()
                }

                throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    @OptIn(TypeRefinement::class)
    fun simpleType(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        kotlinTypeRefiner: KotlinTypeRefiner? = null
    ): SimpleType {
        if (annotations.isEmpty() && arguments.isEmpty() && !nullable && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            annotations, constructor, arguments, nullable,
            computeMemberScope(constructor, arguments, kotlinTypeRefiner)
        ) f@{ refiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, refiner, arguments) ?: return@f null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@f it }

            simpleType(annotations, expandedTypeOrRefinedConstructor.refinedConstructor!!, arguments, nullable, refiner)
        }
    }

    @JvmStatic
    fun TypeAliasDescriptor.computeExpandedType(arguments: List<TypeProjection>): SimpleType {
        return TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false).expand(
            TypeAliasExpansion.create(null, this, arguments), Annotations.EMPTY
        )
    }

    @TypeRefinement
    private fun refineConstructor(
        constructor: TypeConstructor,
        kotlinTypeRefiner: KotlinTypeRefiner,
        arguments: List<TypeProjection>
    ): ExpandedTypeOrRefinedConstructor? {
        val basicDescriptor = constructor.declarationDescriptor
        val descriptor = basicDescriptor?.let { kotlinTypeRefiner.refineDescriptor(it) } ?: return null

        if (descriptor is TypeAliasDescriptor) {
            return ExpandedTypeOrRefinedConstructor(descriptor.computeExpandedType(arguments), null)
        }

        val refinedConstructor = descriptor.typeConstructor.refine(kotlinTypeRefiner)
        return ExpandedTypeOrRefinedConstructor(null, refinedConstructor)
    }

    private class ExpandedTypeOrRefinedConstructor(val expandedType: SimpleType?, val refinedConstructor: TypeConstructor?)

    @JvmStatic
    @OptIn(TypeRefinement::class)
    fun simpleTypeWithNonTrivialMemberScope(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope) { kotlinTypeRefiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, kotlinTypeRefiner, arguments) ?: return@SimpleTypeImpl null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@SimpleTypeImpl it }

            simpleTypeWithNonTrivialMemberScope(
                annotations,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,
                nullable,
                memberScope
            )
        }.let {
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
        refinedTypeFactory: RefinedTypeFactory
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, refinedTypeFactory)
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
    private val refinedTypeFactory: RefinedTypeFactory
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
        return refinedTypeFactory(kotlinTypeRefiner) ?: this
    }
}

// Note: a hack to support class descriptor overwriting in case of K/N forward declaration replacement and other such cases
class SupposititiousSimpleType(private val realType: SimpleType, val overwrittenClass: ClassId) : SimpleType() {

    private fun maybeWrap(newType: SimpleType): SupposititiousSimpleType {
        return if (newType === realType) this
        else SupposititiousSimpleType(newType, overwrittenClass)
    }

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType =
            maybeWrap(realType.replaceAnnotations(newAnnotations))

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
            maybeWrap(realType.makeNullableAsSpecified(newNullability))

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType =
            maybeWrap(realType.refine(kotlinTypeRefiner))

    override val constructor: TypeConstructor = realType.constructor
    override val arguments: List<TypeProjection> = realType.arguments
    override val isMarkedNullable: Boolean = realType.isMarkedNullable
    override val memberScope: MemberScope = realType.memberScope
    override val annotations: Annotations = realType.annotations
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
