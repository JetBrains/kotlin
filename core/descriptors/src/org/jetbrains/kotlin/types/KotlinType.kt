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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker

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
sealed class KotlinType : Annotated {

    abstract val constructor: TypeConstructor
    abstract val arguments: List<TypeProjection>
    abstract val isMarkedNullable: Boolean
    abstract val memberScope: MemberScope

    abstract fun unwrap(): UnwrappedType

    final override fun hashCode(): Int {
        if (isError) return super.hashCode()

        var result = constructor.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + if (isMarkedNullable) 1 else 0
        return result
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinType) return false

        return isMarkedNullable == other.isMarkedNullable && StrictEqualityTypeChecker.strictEqualTypes(unwrap(), other.unwrap())
    }
}

abstract class WrappedType : KotlinType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: KotlinType

    override val annotations: Annotations get() = delegate.annotations
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope

    override final fun unwrap(): UnwrappedType {
        var result = delegate
        while (result is WrappedType) {
            result = result.delegate
        }
        return result as UnwrappedType
    }

    override fun toString(): String {
        if (isComputed()) {
            return delegate.toString()
        }
        else {
            return "<Not computed yet>"
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
sealed class UnwrappedType: KotlinType() {
    abstract fun replaceAnnotations(newAnnotations: Annotations): UnwrappedType
    abstract fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType

    override final fun unwrap(): UnwrappedType = this
}

/**
 * This type represents simple type. If you have pure kotlin code without java classes and dynamic types,
 * then all your types are simple.
 * Or more precisely, all instances are subclasses of [SimpleType] or [WrappedType] (which contains [SimpleType] inside).
 */
abstract class SimpleType : UnwrappedType() {
    abstract override fun replaceAnnotations(newAnnotations: Annotations): SimpleType
    abstract override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType

    override fun toString(): String {
        return buildString {
            for ((annotation, target) in annotations.getAllAnnotations()) {
                append("[", DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation, target), "] ")
            }

            append(constructor)
            if (!arguments.isEmpty()) arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
            if (isMarkedNullable) append("?")
        }
    }
}

// lowerBound is a subtype of upperBound
abstract class FlexibleType(val lowerBound: SimpleType, val upperBound: SimpleType) :
        UnwrappedType(), SubtypingRepresentatives {

    abstract val delegate: SimpleType

    override val subTypeRepresentative: KotlinType
        get() = lowerBound
    override val superTypeRepresentative: KotlinType
        get() = upperBound

    override fun sameTypeConstructor(type: KotlinType) = false

    abstract fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String

    override val annotations: Annotations get() = delegate.annotations
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope

    override fun toString(): String = DescriptorRenderer.DEBUG_TEXT.renderType(this)
}

val KotlinType.isError: Boolean
    get() {
        val unwrapped = unwrap()
        return unwrapped is ErrorUtils.ErrorTypeImpl ||
               (unwrapped is FlexibleType && unwrapped.delegate is ErrorUtils.ErrorTypeImpl)
    }
