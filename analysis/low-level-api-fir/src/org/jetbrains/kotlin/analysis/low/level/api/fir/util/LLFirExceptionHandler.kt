/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExceptionHandler
import org.jetbrains.kotlin.fir.declarations.FirFile

internal object LLFirExceptionHandler : FirExceptionHandler() {
    override fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing {
        throw throwable
    }

    override fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing {
        throw throwable
    }
}