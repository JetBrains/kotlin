/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KtType
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
public sealed class KtAnnotationValue(override val token: KtLifetimeToken) : KtLifetimeOwner {
    public abstract val sourcePsi: KtElement?
}

/**
 * This represents an unsupported expression used as an annotation value.
 */
public class KtUnsupportedAnnotationValue @KtAnalysisApiInternals constructor(
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    override val sourcePsi: KtElement?
        get() = withValidityAssertion { null }
}

/**
 * Array of annotation values. E.g: `@A([1, 2])`
 */
public class KtArrayAnnotationValue @KtAnalysisApiInternals constructor(
    values: Collection<KtAnnotationValue>,
    sourcePsi: KtElement?,
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    public val values: Collection<KtAnnotationValue> = values
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

/**
 * Other annotation used as argument. E.g: `@A(B)` where `B` is annotation too
 */
public class KtAnnotationApplicationValue @KtAnalysisApiInternals constructor(
    annotationValue: KtAnnotationApplicationWithArgumentsInfo,
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    public val annotationValue: KtAnnotationApplicationWithArgumentsInfo = annotationValue
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { annotationValue.psi }
}

/**
 * Class reference used as annotation argument. E.g: `@A(String::class)`
 */
public open class KtKClassAnnotationValue(
    type: KtType,
    classId: ClassId?,
    sourcePsi: KtElement?,
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    /**
     * The referenced [ClassId], if available.
     * The property is useful for error handling, as [KtClassErrorType] currently does not provide a [ClassId].
     */
    public val classId: ClassId? = classId
        get() = withValidityAssertion { field }

    /**
     * The class reference type, e.g. `Array<String>` for the `Array<String>::class` literal.
     */
    public val type: KtType = type
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

/**
 * Some enum entry (enum constant) used as annotation argument. E.g: `@A(Color.RED)`
 */
public class KtEnumEntryAnnotationValue @KtAnalysisApiInternals constructor(
    /**
     * Fully qualified name of used enum entry.
     */
    callableId: CallableId?,
    sourcePsi: KtElement?,
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    public val callableId: CallableId? = callableId
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

/**
 * Some constant value (which may be used as initializer of `const val`) used as annotation argument. It may be String literal, number literal or some simple expression.
 * E.g: `@A(1 +2, "a" + "b")` -- both arguments here are [KtConstantAnnotationValue]
 * @see [KtConstantValue]
 */
public class KtConstantAnnotationValue @KtAnalysisApiInternals constructor(
    constantValue: KtConstantValue,
    token: KtLifetimeToken
) : KtAnnotationValue(token) {
    public val constantValue: KtConstantValue = constantValue
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { constantValue.sourcePsi }
}

/**
 * Render annotation value, resulted string is a valid Kotlin source code.
 */
public fun KtAnnotationValue.renderAsSourceCode(): String =
    KtAnnotationValueRenderer.render(this)
