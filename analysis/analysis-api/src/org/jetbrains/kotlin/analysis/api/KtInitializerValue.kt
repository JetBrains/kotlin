/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Value representing some property or variable initializer
 */
public sealed class KtInitializerValue {
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
public class KtConstantInitializerValue(
    public val constant: KtConstantValue,
    override val initializerPsi: KtExpression?
) : KtInitializerValue()

/**
 * Property initializer which cannot be represented as Kotlin const value.
 *
 * See [KtConstantInitializerValue] for more info.
 */
public class KtNonConstantInitializerValue(
    override val initializerPsi: KtExpression?,
) : KtInitializerValue()

/**
 * Initializer of property of annotation, which can not be which cannot be represented as Kotlin const value,
 *   but can be represented as [KtAnnotationValue]
 */
public class KtConstantValueForAnnotation(
    public val annotationValue: KtAnnotationValue,
    override val initializerPsi: KtExpression?
) : KtInitializerValue()
