/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

/**
 * Annotation values are expected to be compile-time constants. According to the
 * [spec](https://kotlinlang.org/spec/annotations.html#annotation-values),
 * allowed kinds are:
 *   * integer types,
 *   * string type,
 *   * enum types,
 *   * other annotation types, and
 *   * array of aforementioned types
 *
 *  [KtConstantAnnotationValue]  covers first two kinds;
 *  [KtEnumEntryAnnotationValue] corresponds to enum types;
 *  [KtAnnotationApplicationValue] represents annotation types (with annotation fq name and arguments); and
 *  [KtArrayAnnotationValue] abstracts an array of [KtAnnotationValue]s.
 */
public sealed class KtAnnotationValue {
    public abstract val sourcePsi: KtElement?
}


/**
 * This represents an unsupported expression used as an annotation value.
 */
public object KtUnsupportedAnnotationValue : KtAnnotationValue() {
    override val sourcePsi: KtElement? get() = null
}

/**
 * Array of annotation values. E.g: `@A([1, 2])`
 */
public class KtArrayAnnotationValue(
    public val values: Collection<KtAnnotationValue>,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

/**
 * Other annotation used as argument. E.g: `@A(B)` where `B` is annotation too
 */
public class KtAnnotationApplicationValue(
    public val annotationValue: KtAnnotationApplication,
) : KtAnnotationValue() {
    override val sourcePsi: KtElement? get() = annotationValue.psi
}


/**
 * Class reference used as annotation argument. E.g: `@A(String::class)`
 */
public sealed class KtKClassAnnotationValue : KtAnnotationValue() {
    /**
     * Non-local Class reference used as annotation value. E.g: `@A(String::class)`
     */
    public class KtNonLocalKClassAnnotationValue(
        /**
         * Fully qualified name of the class used
         */
        public val classId: ClassId,
        override val sourcePsi: KtElement?,
    ) : KtKClassAnnotationValue()

    /**
     * Non-local class reference used as annotation argument.
     *
     * E.g:
     * ```
     * fun x() {
     *    class Y
     *
     *    @A(B::class)
     *    fun foo() {}
     * }
     * ```
     */
    public class KtLocalKClassAnnotationValue(
        /**
         * [PsiElement] of the class used. As we can get non-local class only for sources, it is always present.
         */
        public val ktClass: KtClassOrObject,
        override val sourcePsi: KtElement?,
    ) : KtKClassAnnotationValue()

    /**
     * Non-existing class reference used as annotation argument. E.g: `@A(NON_EXISTING_CLASS::class)`
     */
    public class KtErrorClassAnnotationValue(
        override val sourcePsi: KtElement?,
        public val unresolvedQualifierName: String?,
    ) : KtKClassAnnotationValue()
}

/**
 * Some enum entry (enum constant) used as annotation argument. E.g: `@A(Color.RED)`
 */
public class KtEnumEntryAnnotationValue(
    /**
     * Fully qualified name of used enum entry.
     */
    public val callableId: CallableId?,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

/**
 * Some constant value (which may be used as initializer of `const val`) used as annotation argument. It may be String literal, number literal or some simple expression.
 * E.g: `@A(1 +2, "a" + "b")` -- both arguments here are [KtConstantAnnotationValue]
 * @see [KtConstantValue]
 */
public class KtConstantAnnotationValue(
    public val constantValue: KtConstantValue,
) : KtAnnotationValue() {
    override val sourcePsi: KtElement? get() = constantValue.sourcePsi
}


/**
 * Render annotation value, resulted string is a valid Kotlin source code.
 */
public fun KtAnnotationValue.renderAsSourceCode(): String =
    KtAnnotationValueRenderer.render(this)
