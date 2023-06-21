/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirStatement

/**
 * Collector needed to collect TowerDataContext in IDE for in-air resolved fir elements.
 */
interface FirResolveContextCollector {
    fun addFileContext(file: FirFile, context: FirTowerDataContext)
    fun addStatementContext(statement: FirStatement, context: BodyResolveContext)
    fun addDeclarationContext(declaration: FirDeclaration, context: BodyResolveContext)

    /**
     * Here by "header" we understand the parts of the class declaration which resolution is not affected
     * by the class own supertypes.
     *
     * It includes any type reference mentioned before the body of the class, except for:
     * - primary constructor declaration with annotations
     * - super type constructor **value arguments** with annotations
     *
     * Also, it includes class type parameters, because they are available anywhere inside the class declaration.
     *
     * N.B. We collect header contexts only for [FirRegularClass]es, ignoring
     * [org.jetbrains.kotlin.fir.declarations.FirAnonymousObject]s. That's because anonymous objects are in
     * fact expressions, and it should be enough for [FirTowerDataContextCollector] to have a context for
     * the containing declaration to correctly process everything inside anonymous objects.
     */
    fun addClassHeaderContext(declaration: FirRegularClass, context: FirTowerDataContext)
}
