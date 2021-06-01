/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.completion.KotlinFirIconProvider.getIconFor
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirTypeConstraintNameInWhereClausePositionContext
import org.jetbrains.kotlin.idea.completion.contributors.helpers.insertSymbolAndInvokeCompletion
import org.jetbrains.kotlin.idea.completion.lookups.KotlinLookupObject
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

internal class FirTypeParameterConstraintNameInWhereClauseCompletionContributor(
    basicContext: FirBasicCompletionContext
) : FirCompletionContributorBase<FirTypeConstraintNameInWhereClausePositionContext>(basicContext) {

    override fun KtAnalysisSession.complete(positionContext: FirTypeConstraintNameInWhereClausePositionContext) {
        val ownerSymbol = positionContext.typeParametersOwner.getSymbol() as? KtSymbolWithTypeParameters ?: return
        ownerSymbol.typeParameters.forEach { typeParameter ->
            val name = typeParameter.name
            val icon = getIconFor(typeParameter)
            LookupElementBuilder.create(TypeParameterInWhenClauseILookupObject(name), name.asString())
                .withTailText(" : ")
                .withInsertHandler(TypeParameterInWhenClauseInsertionHandler)
                .withPsiElement(typeParameter.psi)
                .withIcon(icon)
                .let(sink::addElement)
        }
    }
}

private class TypeParameterInWhenClauseILookupObject(override val shortName: Name) : KotlinLookupObject


private object TypeParameterInWhenClauseInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val lookupElement = item.`object` as TypeParameterInWhenClauseILookupObject
        val name = lookupElement.shortName.render()
        context.document.replaceString(context.startOffset, context.tailOffset, name)
        context.commitDocument()
        context.insertSymbolAndInvokeCompletion(symbol = " : ")
    }
}


