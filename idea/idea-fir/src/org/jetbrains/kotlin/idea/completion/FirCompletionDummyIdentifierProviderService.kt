/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtWhenEntry

class FirCompletionDummyIdentifierProviderService : CompletionDummyIdentifierProviderService() {
    override fun handleDefaultCase(context: CompletionInitializationContext): String? {
        val elementAtOffset = context.file.findElementAt(context.startOffset) ?: return null
        return when {
            elementAtOffset.parentOfType<KtWhenEntry>() != null -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
            else -> null
        }
    }

    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: KtNameReferenceExpression): Boolean {
        return true
        // TODO fir cannot handle invalid code and handles listOf< as binary expression
//        return analyse(nameReferenceExpression) {
//            val reference = nameReferenceExpression.mainReference
//            val targets = reference.resolveToSymbols()
//            targets.isNotEmpty() && targets.all { target ->
//                target is KtFunctionSymbol || target is KtClassOrObjectSymbol && target.classKind == KtClassKind.CLASS
//            }
//        }
    }
}