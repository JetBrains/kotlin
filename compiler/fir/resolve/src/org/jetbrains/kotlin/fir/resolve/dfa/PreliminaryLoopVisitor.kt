/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.expressions.explicitReceiver
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

class PreliminaryLoopVisitor {
    private val reassignedVariablesPerElement: SetMultimap<FirStatement, Name> = setMultimapOf()

    fun enterCapturingStatement(statement: FirStatement): Set<Name> {
        assert(statement is FirLoop || statement is FirClass || statement is FirFunction)
        if (statement !in reassignedVariablesPerElement) {
            statement.accept(visitor, null)
        }
        return reassignedVariablesPerElement[statement]
    }

    fun exitCapturingStatement(statement: FirStatement): Set<Name> {
        assert(statement is FirLoop || statement is FirClass || statement is FirFunction)
        return reassignedVariablesPerElement.removeKey(statement)
    }

    fun resetState() {
        reassignedVariablesPerElement.clear()
    }

    // FirStatement -- closest statement (loop/lambda/local declaration) which may contain reassignments
    private val visitor = object : FirVisitor<Unit, FirStatement?>() {
        override fun visitElement(element: FirElement, data: FirStatement?) {
            element.acceptChildren(this, data)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: FirStatement?) {
            if (variableAssignment.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) return

            // Only care about local variable assignments, which never have explicit receivers. If this is a `var`
            // property assignment, the smart cast will be unstable anyway.
            if (variableAssignment.explicitReceiver != null) return

            val reference = variableAssignment.calleeReference as? FirNamedReference
            if (reference != null) {
                requireNotNull(data)
                reassignedVariablesPerElement.put(data, reference.name)
            }
            visitElement(variableAssignment, data)
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
                reassignedVariablesPerElement.putAll(parent, reassignedVariablesPerElement[statement])
            }
        }
    }
}
