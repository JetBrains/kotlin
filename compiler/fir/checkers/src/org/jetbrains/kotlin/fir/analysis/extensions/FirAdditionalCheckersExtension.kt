/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.extensions.AbstractFirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService

abstract class FirAdditionalCheckersExtension(session: FirSession) : AbstractFirAdditionalCheckersExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("ExtensionCheckers")
    }

    open val declarationCheckers: DeclarationCheckers = DeclarationCheckers.EMPTY
    open val expressionCheckers: ExpressionCheckers = ExpressionCheckers.EMPTY

    final override val name: FirExtensionPointName
        get() = NAME
}

val FirExtensionService.additionalCheckers: List<FirAdditionalCheckersExtension> by FirExtensionService.registeredExtensions(
    AbstractFirAdditionalCheckersExtension::class
)
