/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.codeInsight.intention.PriorityAction
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.intentions.fir.AddAccessorsIntention
import org.jetbrains.kotlin.psi.KtProperty

object AddAccessorsFactories {
    val addAccessorsToUninitializedProperty = diagnosticFixFactory(KtFirDiagnostic.MustBeInitializedOrBeAbstract::class) { diagnostic ->
        val property: KtProperty = diagnostic.psi
        val addGetter = property.getter == null
        val addSetter = property.isVar && property.setter == null
        if (!addGetter && !addSetter) return@diagnosticFixFactory emptyList()

        listOf(
            AddAccessorsIntention(
                addGetter,
                addSetter,
                if (addGetter && addSetter) PriorityAction.Priority.LOW else PriorityAction.Priority.NORMAL
            )
        )
    }
}