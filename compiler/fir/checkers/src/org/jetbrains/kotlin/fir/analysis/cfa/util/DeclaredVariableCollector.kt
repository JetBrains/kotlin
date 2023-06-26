/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// Note that [PreliminaryLoopVisitor] in FIR DFA collects assigned variable names.
// This one collects declared variable symbols per capturing statements.
class DeclaredVariableCollector {
    val declaredVariablesPerElement: SetMultimap<FirStatement, FirPropertySymbol> = setMultimapOf()

    fun enterCapturingStatement(statement: FirStatement): Set<FirPropertySymbol> {
        assert(statement is FirLoop || statement is FirClass || statement is FirFunction)
        if (statement !in declaredVariablesPerElement) {
            statement.accept(visitor, null)
        }
        return declaredVariablesPerElement[statement]
    }

    fun exitCapturingStatement(statement: FirStatement) {
        assert(statement is FirLoop || statement is FirClass || statement is FirFunction)
        declaredVariablesPerElement.removeKey(statement)
    }

    // FirStatement -- closest statement (loop/lambda/local declaration) which may contain reassignments
    private val visitor = object : FirVisitor<Unit, FirStatement?>() {
        override fun visitElement(element: FirElement, data: FirStatement?) {
            element.acceptChildren(this, data)
        }

        override fun visitProperty(property: FirProperty, data: FirStatement?) {
            if (property.isLocal) {
                requireNotNull(data)
                declaredVariablesPerElement.put(data, property.symbol)
            }
            visitElement(property, data)
        }

        override fun visitWhileLoop(whileLoop: FirWhileLoop, data: FirStatement?) {
            visitCapturingStatement(whileLoop, data)
        }

        override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: FirStatement?) {
            visitCapturingStatement(doWhileLoop, data)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: FirStatement?) {
            visitCapturingStatement(anonymousFunction, data)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: FirStatement?) {
            visitCapturingStatement(simpleFunction, data)
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: FirStatement?) {
            visitCapturingStatement(regularClass, data)
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: FirStatement?) {
            visitCapturingStatement(anonymousObject, data)
        }

        private fun visitCapturingStatement(statement: FirStatement, parent: FirStatement?) {
            visitElement(statement, statement)
            if (parent != null) {
                declaredVariablesPerElement.putAll(parent, declaredVariablesPerElement[statement])
            }
        }
    }
}
