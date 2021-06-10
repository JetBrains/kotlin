/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactoriesFromIntentionActions
import org.jetbrains.kotlin.idea.fir.intentions.HLAddGetterAndSetterIntention
import org.jetbrains.kotlin.idea.fir.intentions.HLAddGetterIntention
import org.jetbrains.kotlin.idea.fir.intentions.HLAddSetterIntention
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.psi.KtProperty

object AddAccessorsFactories {
    val addAccessorsToUninitializedProperty =
        diagnosticFixFactoriesFromIntentionActions(
            KtFirDiagnostic.MustBeInitialized::class,
            KtFirDiagnostic.MustBeInitializedOrBeAbstract::class
        ) { diagnostic ->
            val property: KtProperty = diagnostic.psi
            val addGetter = property.getter == null
            val addSetter = property.isVar && property.setter == null
            when {
                addGetter && addSetter -> listOf(HLAddGetterAndSetterIntention())
                addGetter -> listOf(HLAddGetterIntention())
                addSetter -> listOf(HLAddSetterIntention())
                else -> emptyList()
            }
        }
}