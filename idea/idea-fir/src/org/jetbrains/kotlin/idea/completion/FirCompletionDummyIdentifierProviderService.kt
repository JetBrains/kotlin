/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class FirCompletionDummyIdentifierProviderService : CompletionDummyIdentifierProviderService() {
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