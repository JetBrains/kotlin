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
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

/**
 * @see KotlinTypeChecker.isSubtypeOf
 */
interface KotlinType : Annotated {
    val constructor: TypeConstructor

    val arguments: List<TypeProjection>

    val isMarkedNullable: Boolean

    val memberScope: MemberScope

    val isError: Boolean

    override fun equals(other: Any?): Boolean
}

@Deprecated("Temporary marker method for refactoring")
fun KotlinType.asSimpleType(): SimpleType {
    return unwrap() as SimpleType
}

fun KotlinType.unwrap(): KotlinType {
    if (this is WrappedType) return unwrap().unwrap()
    return this
}

interface SimpleType : KotlinType {
    val abbreviatedType : SimpleType? get() = null
}

interface TypeWithCustomReplacement : KotlinType {
    fun makeNullableAsSpecified(nullable: Boolean): KotlinType

    fun replaceAnnotations(newAnnotations: Annotations): KotlinType
}

abstract class WrappedType() : KotlinType, LazyType {
    override val annotations: Annotations get() = delegate.annotations
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope


    open fun isComputed(): Boolean = true
    open val delegate: KotlinType
        get() = unwrap()

    abstract fun unwrap(): KotlinType

    override fun toString(): String {
        if (isComputed()) {
            return delegate.toString()
        }
        else {
            return "<Not computed yet>"
        }
    }

    // todo: remove this later
    override val isError: Boolean get() = delegate.isError
    override fun equals(other: Any?): Boolean = unwrap().equals(other)
    override fun hashCode(): Int = unwrap().hashCode()
}

fun SimpleType.lazyReplaceNullability(newNullable: Boolean): SimpleType {
    if (this is WrappedSimpleType) {
        return WrappedSimpleType(delegate, newAnnotations, newNullable)
    }
    else {
        return WrappedSimpleType(this, newNullable = newNullable)
    }
}

fun SimpleType.lazyReplaceAnnotations(newAnnotations: Annotations): SimpleType {
    if (this is WrappedSimpleType) {
        return WrappedSimpleType(delegate, newAnnotations, newNullable)
    }
    else {
        return WrappedSimpleType(this, newAnnotations)
    }
}

fun KotlinType.getAbbreviatedType(): SimpleType? = (unwrap() as? SimpleType)?.abbreviatedType

fun SimpleType.withAbbreviatedType(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return KotlinTypeImpl.create(annotations, constructor, isMarkedNullable, arguments, memberScope, abbreviatedType)
}

private class WrappedSimpleType(
        override val delegate: SimpleType,
        val newAnnotations: Annotations? = null,
        val newNullable: Boolean? = null
): WrappedType(), SimpleType {
    override val annotations: Annotations
        get() = newAnnotations ?: delegate.annotations

    override val isMarkedNullable: Boolean
        get() = newNullable ?: delegate.isMarkedNullable

    override fun unwrap(): KotlinType {
        if (delegate.isError) return delegate // todo
        if (delegate is CustomTypeVariable) return delegate // todo
        return KotlinTypeImpl.create(annotations, constructor, isMarkedNullable, arguments, memberScope, abbreviatedType)
    }

    override fun toString(): String {
        if (isError) return delegate.toString()

        return buildString {
            for (annotation in annotations.getAllAnnotations()) {
                append("[", DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation.annotation, annotation.target), "] ")
            }

            append(constructor)
            if (!arguments.isEmpty()) arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
            if (isMarkedNullable) append("?")
        }
    }
}