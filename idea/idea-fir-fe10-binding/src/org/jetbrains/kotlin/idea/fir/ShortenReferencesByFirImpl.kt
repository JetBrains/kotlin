/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.ActionRunningMode
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.util.runAction

class ShortenReferencesByFirImpl : ShortenReferences {

    override fun process(element: KtElement, actionRunningMode: ActionRunningMode) {
        val referenceShortenings = runReadAction {
            analyse(element) {
                collectPossibleReferenceShortenings(element.containingKtFile, element.textRange)
            }
        }
        runWriteAction {
            actionRunningMode.runAction { referenceShortenings.invokeShortening() }
        }
    }

    override fun process(elements: Collection<KtElement>, actionRunningMode: ActionRunningMode) {
        runReadAction { elements.groupBy(KtElement::getContainingKtFile) }
            .forEach { shortenReferencesInFile(it.key, it.value, actionRunningMode) }
    }

    private fun shortenReferencesInFile(file: KtFile, elements: List<KtElement>, actionRunningMode: ActionRunningMode) {
        process(file, elements.minOf { it.textRange.startOffset }, elements.maxOf { it.textRange.endOffset }, actionRunningMode)
    }

    override fun process(file: KtFile, startOffset: Int, endOffset: Int, actionRunningMode: ActionRunningMode) {
        val referenceShortenings = runReadAction {
            analyse(file) {
                collectPossibleReferenceShortenings(file, TextRange(startOffset, endOffset))
            }
        }
        runWriteAction {
            actionRunningMode.runAction { referenceShortenings.invokeShortening() }
        }
    }
}