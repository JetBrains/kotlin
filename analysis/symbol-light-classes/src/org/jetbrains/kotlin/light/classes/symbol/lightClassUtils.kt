/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.psi.KtElement

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText(StandardNames.NON_EXISTENT_CLASS.asString(), this)

@OptIn(KtAllowAnalysisOnEdt::class, KtAllowAnalysisFromWriteAction::class)
private inline fun <E> allowLightClassesOnEdt(crossinline action: () -> E): E = allowAnalysisFromWriteAction {
    allowAnalysisOnEdt(action)
}

internal inline fun <R> analyzeForLightClasses(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
    allowLightClassesOnEdt {
        analyze(context, action = action)
    }

internal inline fun <R> analyzeForLightClasses(useSiteKtModule: KtModule, crossinline action: KtAnalysisSession.() -> R): R =
    allowLightClassesOnEdt {
        analyze(useSiteKtModule, action = action)
    }
