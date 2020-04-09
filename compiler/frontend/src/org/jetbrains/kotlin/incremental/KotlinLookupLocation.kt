/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.doNotAnalyze

class KotlinLookupLocation(val element: KtElement) : LookupLocation {

    override val location: LocationInfo?
        get() {
            val containingJetFile = element.containingKtFile
            if (containingJetFile.doNotAnalyze != null) return null
            val virtualFile = containingJetFile.virtualFile ?: return null

            return object : KotlinLocationInfo(virtualFile) {
                override val filePath: String
                    get() = checkNotNull(containingJetFile.virtualFilePath)

                override val position: Position
                    get() = getLineAndColumnInPsiFile(containingJetFile, element.textRange).let { Position(it.line, it.column) }
            }
        }
}
