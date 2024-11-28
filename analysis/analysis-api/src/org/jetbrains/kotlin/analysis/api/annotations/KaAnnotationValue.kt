/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement

/**
 * [KaAnnotationValue] represents the [value](https://kotlinlang.org/spec/annotations.html#annotation-values) of an annotation argument.
 *
 * Annotation values must be compile-time constants. They are restricted in their allowed types, which are:
 *
 * - Integers, [Boolean], and [String] ([ConstantValue])
 * - Enum types ([EnumEntryValue])
 * - [KClass][kotlin.reflect.KClass] ([ClassLiteralValue])
 * - Other annotation types ([NestedAnnotationValue])
 * - Arrays of aforementioned types ([ArrayValue])
 *
 * #### Examples
 *
 * ```kotlin
 * @Foo1(5, true, "foo")                        // Constant values
 * @Foo2(Color.RED)                             // Enum entries
 * @Foo3(String::class)                         // `KClass` values
 * @Foo4(Foo2(Color.RED))                       // Nested annotation values
 * @Foo5([Color.RED, Color.GREEN, Color.BLUE])  // Arrays
 * ```
 */
public sealed interface KaAnnotationValue : KaLifetimeOwner {
    /**
     * The [KtElement] underlying the annotation value. This is only defined for annotations in source files. For libraries, it always
     * returns `null`.
     */
    public val sourcePsi: KtElement?

    /**
     * A constant annotation value, such as `1` or `"foo"`.
     *
     * @see KaConstantValue
     */
    public interface ConstantValue : KaAnnotationValue {
        /**
         * A constant value (a number, [Boolean], [Char], or [String]) wrapped into the [KaConstantValue] abstraction.
         */
        public val value: KaConstantValue
    }

    /**
     * An enum entry annotation value, such as `Color.RED`.
     */
    public interface EnumEntryValue : KaAnnotationValue {
        /**
         * The fully qualified [CallableId] of the enum entry.
         */
        public val callableId: CallableId?
    }

    /**
     * A class literal annotation value, such as `String::class` or `Array<String>::class`.
     */
    public interface ClassLiteralValue : KaAnnotationValue {
        /**
         * The [KaType] of the class reference, such as `String` for the `String::class` reference.
         */
        public val type: KaType

        /**
         * The referenced [ClassId], if available.
         *
         * The property is useful for error handling, as [KaClassErrorType] currently does not provide a [ClassId].
         */
        public val classId: ClassId?
    }

    /**
     * Represents a nested annotation value, such as `ReplaceWith("bar()")` in `@Deprecated("Use 'bar()' instead", ReplaceWith("bar()"))`.
     */
    public interface NestedAnnotationValue : KaAnnotationValue {
        /**
         * The [KaAnnotation] which is applied as an annotation argument through this nested annotation value.
         */
        public val annotation: KaAnnotation
    }

    /**
     * Represents an array of annotation values, such as `arrayOf(1, 2)` or `[1, 2]`.
     */
    public interface ArrayValue : KaAnnotationValue {
        /**
         * The list of annotation values contained in the array.
         */
        public val values: Collection<KaAnnotationValue>
    }

    /**
     * Represents an unsupported expression used as an annotation value.
     */
    public interface UnsupportedValue : KaAnnotationValue
}

/**
 * Renders the annotation value as valid Kotlin source code.
 */
public fun KaAnnotationValue.renderAsSourceCode(): String =
    KaAnnotationValueRenderer.render(this)
