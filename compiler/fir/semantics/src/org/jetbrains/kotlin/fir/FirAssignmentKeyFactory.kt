/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.AssignmentKey
import org.jetbrains.kotlin.fir.resolve.dfa.LexicalScopeKey

abstract class FirAssignmentKeyFactory : FirSessionComponent {
    abstract fun createAssignmentKey(assignment: FirVariableAssignment): AssignmentKey
    abstract fun createAssignmentKey(assignment: FirAugmentedAssignment): AssignmentKey

    abstract fun createLexicalScopeKey(element: FirElement): LexicalScopeKey
}

val FirSession.assignmentKeyFactory: FirAssignmentKeyFactory by FirSession.sessionComponentAccessor()

context(holder: SessionHolder)
val assignmentKeyFactory: FirAssignmentKeyFactory get() = holder.session.assignmentKeyFactory

class FirAssignmentKeyFactoryImpl : FirAssignmentKeyFactory() {
    override fun createAssignmentKey(assignment: FirVariableAssignment): AssignmentKey {
        return FirAssignmentKey.Variable(assignment)
    }

    override fun createAssignmentKey(assignment: FirAugmentedAssignment): AssignmentKey {
        return FirAssignmentKey.Augmented(assignment)
    }

    override fun createLexicalScopeKey(element: FirElement): LexicalScopeKey {
        return FirLexicalScopeKey(element)
    }

    private sealed class FirAssignmentKey : AssignmentKey {
        data class Variable(val fir: FirVariableAssignment) : FirAssignmentKey() {
            override val isAugmentedAssignment: Boolean get() = false
        }

        data class Augmented(val fir: FirAugmentedAssignment) : FirAssignmentKey() {
            override val isAugmentedAssignment: Boolean get() = true
        }
    }

    private data class FirLexicalScopeKey(val fir: FirElement) : LexicalScopeKey
}
