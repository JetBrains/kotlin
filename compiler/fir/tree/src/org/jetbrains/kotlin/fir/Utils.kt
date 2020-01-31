/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.AbstractFirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression

fun ModuleInfo.dependenciesWithoutSelf(): Sequence<ModuleInfo> = dependencies().asSequence().filter { it != this }

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)

private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")

fun AbstractFirRegularClassBuilder.calculateSAM() {
    val status = status as FirDeclarationStatusImpl
    status.isNotSAM = isNotSam()
}

fun AbstractFirRegularClassBuilder.isNotSam(): Boolean {
    var counter = 0
    for (declaration in declarations) {
        if (declaration is FirProperty && declaration.modality == Modality.ABSTRACT) {
            return true
        }
        if (declaration is FirSimpleFunction) {
            if (declaration.modality != Modality.ABSTRACT || declaration.name.asString() in PUBLIC_METHOD_NAMES_IN_OBJECT) {
                continue
            }
            counter++
            if (counter > 1) {
                return true
            }
        }
    }
    return false
}
