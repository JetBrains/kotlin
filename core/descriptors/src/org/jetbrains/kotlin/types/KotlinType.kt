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

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentListMarker

/**
 * [KotlinType] has only two direct subclasses: [WrappedType] and [UnwrappedType].
 *
 * WrappedType is used for lazy computations and for other purposes,
 * where we cannot compute type directly because of lazy initializations.
 * We cannot check type on instanceOf directly, because WrappedType is a wrapper around type.
 *
 * To solve this problem, there is another subclass of KotlinType -- UnwrappedType.
 * So, if you have instance of UnwrappedType, you can safely check this type by instanceOf.
 * To get UnwrappedType you should call method [unwrap].
 *
 * For example, if you want to check, that current type is Flexible, you should run the following:
 * `type.unwrap() is FlexibleType`.
 * For more examples see usages of method [unwrap].
 *
 * For type creation see [KotlinTypeFactory].
 */
sealed class KotlinType : Annotated, KotlinTypeMarker {

    abstract val constructor: TypeConstructor
    abstract val arguments: List<TypeProjection>
    abstract val isMarkedNullable: Boolean
    abstract val memberScope: MemberScope
    abstract val attributes: TypeAttributes
    override val annotations: Annotations
        get() = attributes.annotations

    abstract fun unwrap(): UnwrappedType

    /**
     * Returns refined type using passed KotlinTypeRefiner
     *
     * Refined type has its member scope refined
     *
     * Note #1: supertypes and type arguments ARE NOT refined!
     *
     * Note #2: Correct subtyping or equality for refined types from different Refiners *is not guaranteed*
     *
     * Implementation notice:
     * Basically, this is a simple form of double-dispatching, used to incapsulate
     * structure of specific type-implementations, which means that compound types most probably would like
     * to implement it by recursively calling [refine] on components.
     * A very few "basic" types (like [SimpleTypeImpl]) implement it by actually adjusting
     * content using passed refiner and other low-level methods
     */
    @TypeRefinement
    abstract fun refine(kotlinTypeRefiner: KotlinTypeRefiner): KotlinType

    @TypeRefinement
    open val hasNotTrivialRefinementFactory: Boolean
        get() = false

    /* '0' means "hashCode wasn't computed"

     Note #1. We don't use 'null' as a sign of "uncomputed value" to avoid boxing,
     and even if we get that rumored "integer hashCode collision", we'd just lose
     caching for that "unlucky" instance

     Note #2. We don't use @Volatile even though that field can be accessed concurrently.
     The reason is that contended volatile reads may be harmful for performance,
     and there's no harm in computing this value several times concurrently
     */
    private var cachedHashCode: Int = 0

    private fun computeHashCode(): Int {
        if (isError) return super.hashCode()

        var result = constructor.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + if (isMarkedNullable) 1 else 0
        return result
    }

    final override fun hashCode(): Int {
        // NB: make one read to prevent race
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()

        cachedHashCode = currentHashCode

        return currentHashCode
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinType) return false

        return isMarkedNullable == other.isMarkedNullable && StrictEqualityTypeChecker.strictEqualTypes(unwrap(), other.unwrap())
    }
}

fun KotlinType.isNullable(): Boolean = TypeUtils.isNullableType(this)

abstract class WrappedType : KotlinType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: KotlinType

    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    final override fun unwrap(): UnwrappedType {
        var result = delegate
        while (result is WrappedType) {
            result = result.delegate
        }
        return result as UnwrappedType
    }

    override fun toString(): String {
        return if (isComputed()) {
            delegate.toString()
        } else {
            "<Not computed yet>"
        }
    }
}

/**
 * If you have instance of this type, you can safety check it using instanceOf.
 *
 * WARNING: For wrappers around types you should use only [WrappedType].
 *
 * Methods [replaceAnnotations] and [makeNullableAsSpecified] exist here,
 * because type should save its own internal structure when we want to replace nullability or annotations.
 * For example: nullable captured type still should be captured type.
 *
 * todo: specify what happens with internal structure when we apply some [TypeSubstitutor]
 */
sealed class UnwrappedType : KotlinType() {
    abstract fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType
    abstract fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType

    final override fun unwrap(): UnwrappedType = this

    @TypeRefinement
    abstract override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): UnwrappedType
}

/**
 * This type represents simple type. If you have pure kotlin code without java classes and dynamic types,
 * then all your types are simple.
 * Or more precisely, all instances are subclasses of [SimpleType] or [WrappedType] (which contains [SimpleType] inside).
 */
abstract class SimpleType : UnwrappedType(), SimpleTypeMarker, TypeArgumentListMarker {
    abstract override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType
    abstract override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType

    @TypeRefinement
    abstract override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType

    override fun toString(): String {
        return buildString {
            for (annotation in annotations) {
                append("[", DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation), "] ")
            }

            append(constructor)
            if (arguments.isNotEmpty()) arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
            if (isMarkedNullable) append("?")
        }
    }
}

// lowerBound is a subtype of upperBound
abstract class FlexibleType(val lowerBound: SimpleType, val upperBound: SimpleType) :
    UnwrappedType(), SubtypingRepresentatives, FlexibleTypeMarker {

    abstract val delegate: SimpleType

    override val subTypeRepresentative: KotlinType
        get() = lowerBound
    override val superTypeRepresentative: KotlinType
        get() = upperBound

    override fun sameTypeConstructor(type: KotlinType) = false

    abstract fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String

    override val attributes: TypeAttributes get() = delegate.attributes
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope

    override fun toString(): String = DescriptorRenderer.DEBUG_TEXT.renderType(this)

    @TypeRefinement
    abstract override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): FlexibleType
}

val KotlinType.isError: Boolean
    get() = unwrap().let { unwrapped ->
        unwrapped is ErrorType ||
                (unwrapped is FlexibleType && unwrapped.delegate is ErrorType)
    }
