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
import org.jetbrains.kotlin.types.error.ErrorScope
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ThrowingScope

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
            is TypeAliasDescriptor -> ErrorUtils.createErrorScope(
                ErrorScopeKind.SCOPE_FOR_ABBREVIATION_TYPE, throwExceptions = true, descriptor.name.toString()
            )
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
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        kotlinTypeRefiner: KotlinTypeRefiner? = null
    ): SimpleType {
        if (attributes.isEmpty() && arguments.isEmpty() && !nullable && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            attributes, constructor, arguments, nullable,
            computeMemberScope(constructor, arguments, kotlinTypeRefiner)
        ) f@{ refiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, refiner, arguments) ?: return@f null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@f it }

            simpleType(attributes, expandedTypeOrRefinedConstructor.refinedConstructor!!, arguments, nullable, refiner)
        }
    }

    @JvmStatic
    fun TypeAliasDescriptor.computeExpandedType(arguments: List<TypeProjection>): SimpleType {
        return TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false).expand(
            TypeAliasExpansion.create(null, this, arguments), TypeAttributes.Empty
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
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope) { kotlinTypeRefiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, kotlinTypeRefiner, arguments) ?: return@SimpleTypeImpl null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@SimpleTypeImpl it }

            simpleTypeWithNonTrivialMemberScope(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,
                nullable,
                memberScope
            )
        }.let {
            if (attributes.isEmpty())
                it
            else
                SimpleTypeWithAttributes(it, attributes)
        }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope,
        refinedTypeFactory: RefinedTypeFactory
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, refinedTypeFactory)
            .let {
                if (attributes.isEmpty())
                    it
                else
                    SimpleTypeWithAttributes(it, attributes)
            }

    @JvmStatic
    fun simpleNotNullType(
        attributes: TypeAttributes,
        descriptor: ClassDescriptor,
        arguments: List<TypeProjection>
    ): SimpleType = simpleType(attributes, descriptor.typeConstructor, arguments, nullable = false)

    @JvmStatic
    fun simpleType(
        baseType: SimpleType,
        annotations: TypeAttributes = baseType.attributes,
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
        attributes: TypeAttributes,
        constructor: IntegerLiteralTypeConstructor,
        nullable: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),
        nullable,
        ErrorUtils.createErrorScope(ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE, throwExceptions = true, "unknown integer literal type")
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

    override val attributes: TypeAttributes get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes) =
        if (newAttributes.isEmpty())
            this
        else SimpleTypeWithAttributes(this, newAttributes)

    override fun makeNullableAsSpecified(newNullability: Boolean) = when {
        newNullability == isMarkedNullable -> this
        newNullability -> NullableSimpleType(this)
        else -> NotNullSimpleType(this)
    }

    init {
        if (memberScope is ErrorScope && memberScope !is ThrowingScope) {
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

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        maybeWrap(realType.replaceAttributes(newAttributes))

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
            maybeWrap(realType.makeNullableAsSpecified(newNullability))

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType =
            maybeWrap(realType.refine(kotlinTypeRefiner))

    override val constructor: TypeConstructor = realType.constructor
    override val arguments: List<TypeProjection> = realType.arguments
    override val isMarkedNullable: Boolean = realType.isMarkedNullable
    override val memberScope: MemberScope = realType.memberScope
    override val attributes: TypeAttributes get() = realType.attributes
}

abstract class DelegatingSimpleTypeImpl(override val delegate: SimpleType) : DelegatingSimpleType() {
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        if (newAttributes !== attributes)
            SimpleTypeWithAttributes(this, newAttributes)
        else
            this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        if (newNullability == isMarkedNullable) return this
        return delegate.makeNullableAsSpecified(newNullability).replaceAttributes(attributes)
    }
}

private class SimpleTypeWithAttributes(
    delegate: SimpleType,
    override val attributes: TypeAttributes
) : DelegatingSimpleTypeImpl(delegate) {
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithAttributes(delegate, attributes)
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
