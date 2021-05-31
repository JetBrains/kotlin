/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirClassifierNamePositionContext
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.renderer.render

internal class FirSameAsFileClassifierNameCompletionContributor(
    basicContext: FirBasicCompletionContext
) : FirCompletionContributorBase<FirClassifierNamePositionContext>(basicContext) {

    override fun KtAnalysisSession.complete(positionContext: FirClassifierNamePositionContext) {
        if (positionContext.classLikeDeclaration is KtClassOrObject) {
            completeTopLevelClassName(positionContext.classLikeDeclaration)
        }
    }

    private fun completeTopLevelClassName(classOrObject: KtClassOrObject) {
        if (!classOrObject.isTopLevel()) return
        val name = originalKtFile.virtualFile.nameWithoutExtension
        if (!isValidUpperCapitalizedClassName(name)) return
        if (originalKtFile.declarations.any { it is KtClassOrObject && it.name == name }) return
        sink.addElement(LookupElementBuilder.create(name))
    }

    private fun isValidUpperCapitalizedClassName(name: String) =
        Name.isValidIdentifier(name) && Name.identifier(name).render() == name && name[0].isUpperCase()
}
