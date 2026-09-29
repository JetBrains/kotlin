/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAssignmentKeyFactory
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAugmentedAssignment
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.resolve.dfa.AssignmentKey
import org.jetbrains.kotlin.fir.resolve.dfa.LexicalScopeKey
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class LLFirAssignmentKeyFactory : FirAssignmentKeyFactory() {
    override fun createAssignmentKey(assignment: FirVariableAssignment): AssignmentKey {
        return PsiAssignmentKey.Variable(assignment.psi)
    }

    override fun createAssignmentKey(assignment: FirAugmentedAssignment): AssignmentKey {
        return PsiAssignmentKey.Augmented(assignment.psi)
    }

    override fun createLexicalScopeKey(element: FirElement): LexicalScopeKey {
        return PsiLexicalScopeKey(element.psi)
    }

    private val FirElement.psi: PsiElement
        get() {
            val element = this
            return source?.psi ?: errorWithAttachment("No PSI for ${element::class.simpleName}") {
                withFirEntry("element", element)
            }
        }

    private sealed class PsiAssignmentKey : AssignmentKey {
        data class Variable(val psi: PsiElement) : PsiAssignmentKey() {
            override val isAugmentedAssignment: Boolean get() = false
        }

        data class Augmented(val psi: PsiElement) : PsiAssignmentKey() {
            override val isAugmentedAssignment: Boolean get() = true
        }
    }

    private data class PsiLexicalScopeKey(val psi: PsiElement) : LexicalScopeKey
}
