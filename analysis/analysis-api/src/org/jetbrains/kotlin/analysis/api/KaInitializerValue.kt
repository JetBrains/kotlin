/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtExpression

/**
 * A value of a property or variable initializer.
 *
 * @see org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol.initializer
 */
@KaExperimentalApi
public sealed class KaInitializerValue {
    /**
     * The [KtExpression] backing the initializer. It may be `null` if the property/variable comes from a non-source file.
     */
    public abstract val initializerPsi: KtExpression?
}

/**
 * An initializer value which can be evaluated to a [compile-time constant](https://kotlinlang.org/docs/properties.html#compile-time-constants),
 * such as a string value, number, or `null` literal.
 */
@KaExperimentalApi
public class KaConstantInitializerValue(
    public val constant: KaConstantValue,
    override val initializerPsi: KtExpression?,
) : KaInitializerValue()

/**
 * An initializer value which cannot be represented as a [compile-time constant](https://kotlinlang.org/docs/properties.html#compile-time-constants).
 *
 * The Analysis API is unable to evaluate the expression statically and thus cannot provide a [KaConstantValue] for the initializer.
 *
 * @see KaConstantInitializerValue
 */
@KaExperimentalApi
public class KaNonConstantInitializerValue(
    override val initializerPsi: KtExpression?,
) : KaInitializerValue()

/**
 * An initializer value of a property of an annotation, which cannot be represented as a
 * [compile-time constant](https://kotlinlang.org/docs/properties.html#compile-time-constants), but can be represented as
 * a [KaAnnotationValue].
 */
@KaExperimentalApi
public class KaConstantValueForAnnotation(
    public val annotationValue: KaAnnotationValue,
    override val initializerPsi: KtExpression?,
) : KaInitializerValue()
