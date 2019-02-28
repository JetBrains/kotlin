/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.name.Name

//class FirLocalScope : FirScope {
//
//    val properties = mutableMapOf<Name, FirVariable>()
//    val functions = mutableMapOf<Name, FirNamedFunction>()
//
//    fun storeDeclaration(declaration: FirNamedDeclaration) {
//        when (declaration) {
//            is FirVariable -> properties[declaration.name] = declaration
//            is FirNamedFunction -> functions[declaration.name] = declaration
//
//        }
//    }
//
//    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
//        return super.processFunctionsByName(name, processor)
//    }
//
//    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
//        val prop = properties[name]
//        if (prop != null) {
//            return processor()
//        }
//        return ProcessorAction.NEXT
//    }
//}