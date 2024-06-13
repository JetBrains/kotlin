/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Value representing some property or variable initializer
 */
public sealed class KaInitializerValue {
    /**
     * [com.intellij.psi.PsiElement] of initializer. May be null if property/variable came from non-source file.
     */
    public abstract val initializerPsi: KtExpression?
}

/**
 * Initializer value which can be evaluated to constant. E.g, string value, number, null literal.
 *
 * For more info about constant values please see [official Kotlin documentation](https://kotlinlang.org/docs/properties.html#compile-time-constants]).
 */
public class KaConstantInitializerValue(
    public val constant: KaConstantValue,
    override val initializerPsi: KtExpression?
) : KaInitializerValue()

/**
 * Property initializer which cannot be represented as Kotlin const value.
 *
 * See [KaConstantInitializerValue] for more info.
 */
public class KaNonConstantInitializerValue(
    override val initializerPsi: KtExpression?,
) : KaInitializerValue()

/**
 * Initializer of property of annotation, which can not be which cannot be represented as Kotlin const value,
 *   but can be represented as [KaAnnotationValue]
 */
public class KaConstantValueForAnnotation(
    public val annotationValue: KaAnnotationValue,
    override val initializerPsi: KtExpression?
) : KaInitializerValue()
