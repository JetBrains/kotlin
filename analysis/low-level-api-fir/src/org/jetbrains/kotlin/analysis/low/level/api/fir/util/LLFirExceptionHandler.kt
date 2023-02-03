/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExceptionHandler
import org.jetbrains.kotlin.fir.declarations.FirFile

internal object LLFirExceptionHandler : FirExceptionHandler(), ModificationTracker {
    @Volatile
    private var counter = 0L

    override fun getModificationCount(): Long {
        return counter
    }

    override fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing {
        counter += 1
        throw throwable
    }

    override fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing {
        counter += 1
        throw throwable
    }
}