/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
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
 *  [KaConstantAnnotationValue]  covers first two kinds;
 *  [KaEnumEntryAnnotationValue] corresponds to enum types;
 *  [KaAnnotationApplicationValue] represents annotation types (with annotation fq name and arguments); and
 *  [KaArrayAnnotationValue] abstracts an array of [KaAnnotationValue]s.
 */
public sealed class KaAnnotationValue(override val token: KaLifetimeToken) : KaLifetimeOwner {
    public abstract val sourcePsi: KtElement?
}

public typealias KtAnnotationValue = KaAnnotationValue

/**
 * This represents an unsupported expression used as an annotation value.
 */
public class KaUnsupportedAnnotationValue @KaAnalysisApiInternals constructor(
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    override val sourcePsi: KtElement?
        get() = withValidityAssertion { null }
}

public typealias KtUnsupportedAnnotationValue = KaUnsupportedAnnotationValue

/**
 * Array of annotation values. E.g: `@A([1, 2])`
 */
public class KaArrayAnnotationValue @KaAnalysisApiInternals constructor(
    values: Collection<KaAnnotationValue>,
    sourcePsi: KtElement?,
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    public val values: Collection<KaAnnotationValue> = values
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

public typealias KtArrayAnnotationValue = KaArrayAnnotationValue

/**
 * Other annotation used as argument. E.g: `@A(B)` where `B` is annotation too
 */
public class KaAnnotationApplicationValue @KaAnalysisApiInternals constructor(
    annotationValue: KaAnnotationApplicationWithArgumentsInfo,
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    public val annotationValue: KaAnnotationApplicationWithArgumentsInfo = annotationValue
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { annotationValue.psi }
}

public typealias KtAnnotationApplicationValue = KaAnnotationApplicationValue

/**
 * Class reference used as annotation argument. E.g: `@A(String::class)`
 */
public open class KaKClassAnnotationValue(
    type: KaType,
    classId: ClassId?,
    sourcePsi: KtElement?,
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    /**
     * The referenced [ClassId], if available.
     * The property is useful for error handling, as [KaClassErrorType] currently does not provide a [ClassId].
     */
    public val classId: ClassId? = classId
        get() = withValidityAssertion { field }

    /**
     * The class reference type, e.g. `Array<String>` for the `Array<String>::class` literal.
     */
    public val type: KaType = type
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

public typealias KtKClassAnnotationValue = KaKClassAnnotationValue

/**
 * Some enum entry (enum constant) used as annotation argument. E.g: `@A(Color.RED)`
 */
public class KaEnumEntryAnnotationValue @KaAnalysisApiInternals constructor(
    /**
     * Fully qualified name of used enum entry.
     */
    callableId: CallableId?,
    sourcePsi: KtElement?,
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    public val callableId: CallableId? = callableId
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement? = sourcePsi
        get() = withValidityAssertion { field }
}

public typealias KtEnumEntryAnnotationValue = KaEnumEntryAnnotationValue

/**
 * Some constant value (which may be used as initializer of `const val`) used as annotation argument. It may be String literal, number literal or some simple expression.
 * E.g: `@A(1 +2, "a" + "b")` -- both arguments here are [KaConstantAnnotationValue]
 * @see [KaConstantValue]
 */
public class KaConstantAnnotationValue @KaAnalysisApiInternals constructor(
    constantValue: KaConstantValue,
    token: KaLifetimeToken
) : KaAnnotationValue(token) {
    public val constantValue: KaConstantValue = constantValue
        get() = withValidityAssertion { field }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { constantValue.sourcePsi }
}

public typealias KtConstantAnnotationValue = KaConstantAnnotationValue

/**
 * Render annotation value, resulted string is a valid Kotlin source code.
 */
public fun KaAnnotationValue.renderAsSourceCode(): String =
    KaAnnotationValueRenderer.render(this)
