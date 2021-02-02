/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic

class KtQuickFixService {
    private val list = KtQuickFixesList.createCombined(KtQuickFixRegistrar.allQuickFixesList())

    fun getQuickFixesFor(diagnostic: KtFirDiagnostic): List<IntentionAction> =
        list.getQuickFixesFor(diagnostic)
}

abstract class KtQuickFixRegistrar {
    protected abstract val list: KtQuickFixesList

    companion object {
        private val EP_NAME: ExtensionPointName<KtQuickFixRegistrar> =
            ExtensionPointName.create("org.jetbrains.kotlin.ktQuickFixRegistrar")

        fun allQuickFixesList() = EP_NAME.extensionList.map { it.list }
    }
}