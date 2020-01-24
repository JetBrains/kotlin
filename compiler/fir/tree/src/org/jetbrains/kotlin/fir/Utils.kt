/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableClass
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression

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

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

fun FirModifiableClass<FirRegularClass>.calculateSAM() {
    val status = symbol.fir.status as FirDeclarationStatusImpl
    var counter = 0
    for (declaration in declarations) {
        if (declaration is FirProperty && declaration.modality == Modality.ABSTRACT) {
            status.isNotSAM = true
            return
        }
        if (declaration is FirSimpleFunction) {
            if (declaration.modality != Modality.ABSTRACT || declaration.name.asString() in PUBLIC_METHOD_NAMES_IN_OBJECT) {
                continue
            }
            counter++
            if (counter > 1) {
                status.isNotSAM = true
                return
            }
        }
    }
}