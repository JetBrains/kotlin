/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.psi.KtElement

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText(StandardNames.NON_EXISTENT_CLASS.asString(), this)

@OptIn(KaAllowAnalysisOnEdt::class, KaAllowAnalysisFromWriteAction::class)
private inline fun <E> allowLightClassesOnEdt(crossinline action: () -> E): E = allowAnalysisFromWriteAction {
    allowAnalysisOnEdt(action)
}

internal inline fun <R> analyzeForLightClasses(context: KtElement, crossinline action: KaSession.() -> R): R =
    allowLightClassesOnEdt {
        analyze(context, action = action)
    }

internal inline fun <R> analyzeForLightClasses(useSiteKtModule: KaModule, crossinline action: KaSession.() -> R): R =
    allowLightClassesOnEdt {
        analyze(useSiteKtModule, action = action)
    }
