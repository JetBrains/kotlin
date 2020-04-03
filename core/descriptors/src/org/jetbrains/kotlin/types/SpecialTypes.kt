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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.checker.NullabilityChecker
import org.jetbrains.kotlin.types.model.DefinitelyNotNullTypeMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.canHaveUndefinedNullability

abstract class DelegatingSimpleType : SimpleType() {
    protected abstract val delegate: SimpleType

    override val annotations: Annotations get() = delegate.annotations
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope

    @TypeRefinement
    abstract fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType =
        replaceDelegate(kotlinTypeRefiner.refineType(delegate) as SimpleType)
}

class AbbreviatedType(override val delegate: SimpleType, val abbreviation: SimpleType) : DelegatingSimpleType() {
    val expandedType: SimpleType get() = delegate

    override fun replaceAnnotations(newAnnotations: Annotations)
            = AbbreviatedType(delegate.replaceAnnotations(newAnnotations), abbreviation)

    override fun makeNullableAsSpecified(newNullability: Boolean)
            = AbbreviatedType(delegate.makeNullableAsSpecified(newNullability), abbreviation.makeNullableAsSpecified(newNullability))

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = AbbreviatedType(delegate, abbreviation)

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): AbbreviatedType =
        AbbreviatedType(
            kotlinTypeRefiner.refineType(delegate) as SimpleType,
            kotlinTypeRefiner.refineType(abbreviation) as SimpleType
        )
}

fun KotlinType.getAbbreviatedType(): AbbreviatedType? = unwrap() as? AbbreviatedType
fun KotlinType.getAbbreviation(): SimpleType? = getAbbreviatedType()?.abbreviation

fun SimpleType.withAbbreviation(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return AbbreviatedType(this, abbreviatedType)
}

class LazyWrappedType(
    private val storageManager: StorageManager,
    private val computation: () -> KotlinType
) : WrappedType() {
    private val lazyValue = storageManager.createLazyValue(computation)

    override val delegate: KotlinType get() = lazyValue()

    override fun isComputed(): Boolean = lazyValue.isComputed()

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = LazyWrappedType(storageManager) {
        kotlinTypeRefiner.refineType(computation())
    }
}

class DefinitelyNotNullType private constructor(val original: SimpleType) : DelegatingSimpleType(), CustomTypeVariable,
    DefinitelyNotNullTypeMarker {

    companion object {
        internal fun makeDefinitelyNotNull(type: UnwrappedType): DefinitelyNotNullType? {
            return when {
                type is DefinitelyNotNullType -> type

                makesSenseToBeDefinitelyNotNull(type) -> {
                    if (type is FlexibleType) {
                        assert(type.lowerBound.constructor == type.upperBound.constructor) {
                            "DefinitelyNotNullType for flexible type ($type) can be created only from type variable with the same constructor for bounds"
                        }
                    }


                    DefinitelyNotNullType(type.lowerIfFlexible())
                }

                else -> null
            }
        }

        private fun makesSenseToBeDefinitelyNotNull(type: UnwrappedType): Boolean =
            type.canHaveUndefinedNullability() && !NullabilityChecker.isSubtypeOfAny(type)
    }

    override val delegate: SimpleType
        get() = original

    override val isMarkedNullable: Boolean
        get() = false

    override val isTypeVariable: Boolean
        get() = delegate.constructor is NewTypeVariableConstructor ||
                delegate.constructor.declarationDescriptor is TypeParameterDescriptor

    override fun substitutionResult(replacement: KotlinType): KotlinType =
            replacement.unwrap().makeDefinitelyNotNullOrNotNull()

    override fun replaceAnnotations(newAnnotations: Annotations): DefinitelyNotNullType =
            DefinitelyNotNullType(delegate.replaceAnnotations(newAnnotations))

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
            if (newNullability) delegate.makeNullableAsSpecified(newNullability) else this

    override fun toString(): String = "$delegate!!"

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = DefinitelyNotNullType(delegate)
}

val KotlinType.isDefinitelyNotNullType: Boolean
    get() = unwrap() is DefinitelyNotNullType

fun SimpleType.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleType =
    DefinitelyNotNullType.makeDefinitelyNotNull(this)
        ?: makeIntersectionTypeDefinitelyNotNullOrNotNull()
        ?: makeNullableAsSpecified(false)

fun NewCapturedType.withNotNullProjection() =
    NewCapturedType(captureStatus, constructor, lowerType, annotations, isMarkedNullable, isProjectionNotNull = true)

fun UnwrappedType.makeDefinitelyNotNullOrNotNull(): UnwrappedType =
    DefinitelyNotNullType.makeDefinitelyNotNull(this)
        ?: makeIntersectionTypeDefinitelyNotNullOrNotNull()
        ?: makeNullableAsSpecified(false)

private fun KotlinType.makeIntersectionTypeDefinitelyNotNullOrNotNull(): SimpleType? {
    val typeConstructor = constructor as? IntersectionTypeConstructor ?: return null
    val definitelyNotNullConstructor = typeConstructor.makeDefinitelyNotNullOrNotNull() ?: return null

    return definitelyNotNullConstructor.createType()
}

private fun IntersectionTypeConstructor.makeDefinitelyNotNullOrNotNull(): IntersectionTypeConstructor? {
    return transformComponents({ TypeUtils.isNullableType(it) }, { it.unwrap().makeDefinitelyNotNullOrNotNull() })
}
