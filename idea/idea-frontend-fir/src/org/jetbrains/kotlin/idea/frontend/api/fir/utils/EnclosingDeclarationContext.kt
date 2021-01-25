/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

internal sealed class EnclosingDeclarationContext {
    companion object {
        fun detect(originalFile: KtFile, positionInFakeFile: KtElement): EnclosingDeclarationContext {
            val fakeFunction = positionInFakeFile.getNonStrictParentOfType<KtNamedFunction>()
            if (fakeFunction != null) {
                val originalFunction = originalFile.findDeclarationOfTypeAt<KtNamedFunction>(fakeFunction.textOffset)
                    ?: error("Cannot find original function matching to ${fakeFunction.getElementTextInContext()} in $originalFile")
                recordOriginalDeclaration(originalFunction, fakeFunction)
                return FunctionContext(fakeFunction, originalFunction)
            }

            val fakeProperty = positionInFakeFile.parentsOfType<KtProperty>().firstOrNull { !it.isLocal }
            if (fakeProperty != null) {
                val originalProperty = originalFile.findDeclarationOfTypeAt<KtProperty>(fakeProperty.textOffset)
                    ?: error("Cannot find original property matching to ${fakeProperty.getElementTextInContext()} in $originalFile")
                recordOriginalDeclaration(originalProperty, fakeProperty)
                return PropertyContext(fakeProperty, originalProperty)
            }

            error("Cannot find enclosing declaration for ${positionInFakeFile.getElementTextInContext()}")
        }

        private fun recordOriginalDeclaration(originalDeclaration: KtNamedDeclaration, fakeDeclaration: KtNamedDeclaration) {
            require(!fakeDeclaration.isPhysical)
            require(originalDeclaration.containingKtFile !== fakeDeclaration.containingKtFile)
            val originalDeclrationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
            val fakeDeclarationParents = fakeDeclaration.parentsOfType<KtDeclaration>().toList()

            originalDeclrationParents.zip(fakeDeclarationParents) { original, fake ->
                fake.originalDeclaration = original
            }
        }
    }
}

internal class FunctionContext(
    val fakeEnclosingFunction: KtNamedFunction,
    val originalEnclosingFunction: KtNamedFunction
) : EnclosingDeclarationContext()

internal class PropertyContext(
    val fakeEnclosingProperty: KtProperty,
    val originalEnclosingProperty: KtProperty
) : EnclosingDeclarationContext()

internal val EnclosingDeclarationContext.fakeEnclosingDeclaration: KtCallableDeclaration
    get() = when (this) {
        is FunctionContext -> fakeEnclosingFunction
        is PropertyContext -> fakeEnclosingProperty
    }

internal fun EnclosingDeclarationContext.recordCompletionContext(originalFirFile: FirFile, firResolveState: FirModuleResolveState) {
    when (this) {
        is FunctionContext -> LowLevelFirApiFacadeForCompletion.recordCompletionContextForFunction(
            originalFirFile,
            fakeEnclosingFunction,
            originalEnclosingFunction,
            state = firResolveState
        )

        is PropertyContext -> LowLevelFirApiFacadeForCompletion.recordCompletionContextForProperty(
            originalFirFile,
            fakeEnclosingProperty,
            originalEnclosingProperty,
            state = firResolveState
        )
    }
}

private inline fun <reified T : KtElement> KtFile.findDeclarationOfTypeAt(offset: Int): T? =
    findElementAt(offset)
        ?.getNonStrictParentOfType<T>()
        ?.takeIf { it.textOffset == offset }
