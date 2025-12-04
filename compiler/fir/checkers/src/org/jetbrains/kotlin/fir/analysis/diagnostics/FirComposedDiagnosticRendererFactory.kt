/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

class FirRegisteredDiagnosticFactoriesStorage(val storage: KtRegisteredDiagnosticFactoriesStorage) : FirSessionComponent

private val FirSession.firRegisteredDiagnosticFactories: FirRegisteredDiagnosticFactoriesStorage by FirSession.sessionComponentAccessor()
val FirSession.registeredDiagnosticFactoriesStorage: KtRegisteredDiagnosticFactoriesStorage
    get() = firRegisteredDiagnosticFactories.storage
