/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext

interface FirTowerDataContextCollector {
    fun addFileContext(file: FirFile, context: FirTowerDataContext)
    fun addStatementContext(statement: FirStatement, context: FirTowerDataContext)
    fun addDeclarationContext(declaration: FirDeclaration, context: FirTowerDataContext)
}