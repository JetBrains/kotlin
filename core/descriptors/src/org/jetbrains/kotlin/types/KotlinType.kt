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
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

/**
 * @see KotlinTypeChecker.isSubtypeOf
 */
sealed class KotlinType : Annotated {

    abstract val constructor: TypeConstructor
    abstract val arguments: List<TypeProjection>
    abstract val isMarkedNullable: Boolean
    abstract val memberScope: MemberScope
    abstract val isError: Boolean

    abstract fun unwrap(): UnwrappedType

    // ------- internal staff ------

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

        return isMarkedNullable == other.isMarkedNullable && KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(this, other)
    }
}

abstract class WrappedType() : KotlinType(), LazyType {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: KotlinType

    override val annotations: Annotations get() = delegate.annotations
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope
    override val isError: Boolean get() = delegate.isError

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

sealed class UnwrappedType: KotlinType() {
    abstract fun replaceAnnotations(newAnnotations: Annotations): UnwrappedType
    abstract fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType

    override final fun unwrap(): UnwrappedType = this
}

abstract class SimpleType : UnwrappedType() {
    abstract override fun replaceAnnotations(newAnnotations: Annotations): SimpleType
    abstract override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType

    override fun toString(): String {
        // for error types this method should be overridden
        if (isError) return "ErrorType"

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
    override val isError: Boolean get() = false

    override fun toString(): String = DescriptorRenderer.DEBUG_TEXT.renderType(this)
}

