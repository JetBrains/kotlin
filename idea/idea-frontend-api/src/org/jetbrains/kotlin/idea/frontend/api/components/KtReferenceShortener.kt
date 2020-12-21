/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtUserType

abstract class KtReferenceShortener : KtAnalysisSessionComponent() {
    abstract fun collectShortenings(file: KtFile, from: Int, to: Int): ShortenCommand
}

class ShortenCommand(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>
) {
    fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (typePointer in typesToShorten) {
            val type = typePointer.element ?: continue
            type.deleteQualifier()
        }
    }
}