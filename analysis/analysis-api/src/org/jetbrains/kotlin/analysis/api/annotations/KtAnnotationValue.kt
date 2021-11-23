/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
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
public sealed class KtAnnotationValue(
    public open val sourcePsi: KtElement? = null
)


/**
 * This represents an unsupported expression used as an annotation value.
 */
public object KtUnsupportedAnnotationValue : KtAnnotationValue()

public class KtArrayAnnotationValue(
    public val values: Collection<KtAnnotationValue>,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

public class KtAnnotationApplicationValue(
    public val annotationValue: KtAnnotationApplication,
) : KtAnnotationValue() {
    override val sourcePsi: KtElement? get() = annotationValue.psi
}


public class KtEnumEntryAnnotationValue(
    public val callableId: CallableId?,
    override val sourcePsi: KtElement?,
) : KtAnnotationValue()

public class KtConstantAnnotationValue(
    public val constantValue: KtConstantValue,
) : KtAnnotationValue()

public fun KtAnnotationValue.render(): String =
    KtAnnotationValueRenderer.render(this)