/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirFile

abstract class FirExceptionHandler : FirSessionComponent {
    abstract fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing
    abstract fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing
}

val FirSession.exceptionHandler: FirExceptionHandler by FirSession.sessionComponentAccessor()

object FirCliExceptionHandler : FirExceptionHandler() {
    override fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing {
        throw throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element.source)
    }

    override fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing {
        throw throwable.wrapIntoFileAnalysisExceptionIfNeeded(
            file.sourceFile?.path,
            file.source,
        ) { file.sourceFileLinesMapping?.getLineAndColumnByOffset(it) }
    }
}

inline fun <R> whileAnalysing(session: FirSession, element: FirElement, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        session.exceptionHandler.handleExceptionOnElementAnalysis(element, throwable)
    }
}

inline fun <R> withFileAnalysisExceptionWrapping(file: FirFile, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        file.moduleData.session.exceptionHandler.handleExceptionOnFileAnalysis(file, throwable)
    }
}
