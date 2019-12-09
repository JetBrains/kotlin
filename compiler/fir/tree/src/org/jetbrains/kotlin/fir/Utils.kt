/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

fun ModuleInfo.dependenciesWithoutSelf(): Sequence<ModuleInfo> = dependencies().asSequence().filter { it != this }

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)

fun FirElement.unwrapWhenSubjectExpression(): FirElement = if (this is FirWhenSubjectExpression) {
    val whenExpression = whenSubject.whenExpression
    whenExpression.subjectVariable
        ?: whenExpression.subject
        ?: throw IllegalStateException("Subject or subject variable must be not null")
} else {
    this
}

tailrec fun FirElement.unwrapSmartcast(): FirElement = if (this is FirExpressionWithSmartcast) {
    originalExpression.unwrapSmartcast()
} else {
    this
}