/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.helpers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.lookups.shortenReferencesForFirCompletion
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

internal object FirSuperEntriesProvider {
    fun KtAnalysisSession.getSuperClassesAvailableForSuperCall(context: PsiElement): List<KtNamedClassOrObjectSymbol> {
        val containingClass = context.getStrictParentOfType<KtClassOrObject>() ?: return emptyList()
        val containingClassSymbol = containingClass.getClassOrObjectSymbol()
        return containingClassSymbol.superTypes.mapNotNull { superType ->
            val classType = superType.type as? KtClassType ?: return@mapNotNull null
            classType.classSymbol as? KtNamedClassOrObjectSymbol
        }
    }
}

internal object SuperCallInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val lookupObject = item.`object` as SuperCallLookupObject

        replaceWithClassIdAndShorten(lookupObject, context)
        context.insertSymbolAndInvokeCompletion(symbol = ".")
    }

    private fun replaceWithClassIdAndShorten(
        lookupObject: SuperCallLookupObject,
        context: InsertionContext
    ) {
        val replaceTo = lookupObject.replaceTo ?: return
        context.document.replaceString(context.startOffset, context.tailOffset, replaceTo)
        context.commitDocument()

        if (lookupObject.shortenReferencesInReplaced) {
            val targetFile = context.file as KtFile
            shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
        }
    }
}

internal interface SuperCallLookupObject {
    val replaceTo: String?
    val shortenReferencesInReplaced: Boolean
}