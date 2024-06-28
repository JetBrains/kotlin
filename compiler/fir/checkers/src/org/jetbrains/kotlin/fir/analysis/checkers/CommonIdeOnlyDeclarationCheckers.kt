/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirActualAnnotationsMatchExpectChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker

/**
 * Checkers, which only run in IDE and don't run in CLI mode.
 */
object CommonIdeOnlyDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(FirActualAnnotationsMatchExpectChecker)
}