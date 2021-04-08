/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

abstract class BeforeElementDiagnosticCollectionHandler: FirSessionComponent {
    open fun beforeCollectingForElement(element: FirElement) {}
    open fun beforeGoingNestedDeclaration(declaration: FirDeclaration, context: PersistentCheckerContext) {}
}

val FirSession.beforeElementDiagnosticCollectionHandler: BeforeElementDiagnosticCollectionHandler? by FirSession.nullableSessionComponentAccessor()
