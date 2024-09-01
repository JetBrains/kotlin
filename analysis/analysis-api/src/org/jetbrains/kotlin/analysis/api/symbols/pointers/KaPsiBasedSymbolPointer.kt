/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Segment
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.*
import java.lang.ref.SoftReference
import kotlin.reflect.KClass

@KaImplementationDetail
public class KaPsiBasedSymbolPointer<S : KaSymbol> private constructor(
    private val psiPointer: SmartPsiElementPointer<out KtElement>,
    private val expectedClass: KClass<S>,
) : KaSymbolPointer<S>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): S? {
        val psi = psiPointer.element ?: return null

        val symbol: KaSymbol = with(analysisSession) {
            if (!psi.canBeAnalysed()) return null
            when (psi) {
                is KtDeclaration -> psi.symbol
                is KtFile -> psi.symbol
                else -> {
                    error("Unexpected declaration to restore: ${psi::class}, text:\n ${psi.text}")
                }
            }
        }

        if (!expectedClass.isInstance(symbol)) return null

        @Suppress("UNCHECKED_CAST")
        return symbol as S
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaPsiBasedSymbolPointer &&
            other.expectedClass == expectedClass &&
            other.psiPointer == psiPointer

    public constructor(psi: KtElement, expectedClass: KClass<S>) : this(createCompatibleSmartPointer(psi), expectedClass)

    public companion object {
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        public inline fun <reified S : KaSymbol> createForSymbolFromSource(symbol: @kotlin.internal.NoInfer S): KaPsiBasedSymbolPointer<S>? {
            return createForSymbolFromSource(symbol, S::class)
        }

        public fun <S : KaSymbol> createForSymbolFromSource(symbol: S, expectedClass: KClass<S>): KaPsiBasedSymbolPointer<S>? {
            ifDisabled { return null }

            if (symbol.origin != KaSymbolOrigin.SOURCE) return null

            val psi = when (val psi = symbol.psi) {
                is KtDeclaration -> psi
                is KtFile -> psi
                is KtObjectLiteralExpression -> psi.objectDeclaration
                else -> return null
            }

            return KaPsiBasedSymbolPointer(psi, expectedClass)
        }


        public fun <S : KaSymbol> createForSymbolFromPsi(ktElement: KtElement, expectedClass: KClass<S>): KaPsiBasedSymbolPointer<S>? {
            ifDisabled { return null }

            return KaPsiBasedSymbolPointer(ktElement, expectedClass)
        }

        public inline fun <reified S : KaSymbol> createForSymbolFromPsi(ktElement: KtElement): KaPsiBasedSymbolPointer<S>? {
            return createForSymbolFromPsi(ktElement, S::class)
        }

        @TestOnly
        public fun <T> withDisabledPsiBasedPointers(disable: Boolean, action: () -> T): T = try {
            disablePsiPointer = true
            disablePsiPointerFlag.set(disable)
            action()
        } finally {
            disablePsiPointerFlag.remove()
        }

        private inline fun ifDisabled(action: () -> Unit) {
            if (!disablePsiPointer) return
            if (disablePsiPointerFlag.get()) {
                action()
            }
        }

        private val disablePsiPointerFlag: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        @Volatile
        private var disablePsiPointer: Boolean = false
    }
}

@KaImplementationDetail
public interface SmartPointerIncompatiblePsiFile

@OptIn(KaImplementationDetail::class)
private fun createCompatibleSmartPointer(element: KtElement): SmartPsiElementPointer<out KtElement> {
    val containingFile = element.containingKtFile

    if (containingFile is SmartPointerIncompatiblePsiFile) {
        return SoftSmartPsiElementPointer(element, containingFile)
    }

    return SmartPointerManager.getInstance(containingFile.project)
        .createSmartPsiElementPointer(element, containingFile)
}

private class SoftSmartPsiElementPointer<T : PsiElement>(
    element: T,
    containingFile: PsiFile
) : SmartPsiElementPointer<T> {
    private val project = containingFile.project
    private val elementRef = SoftReference(element)
    private val containingFileRef = SoftReference(containingFile)

    override fun getElement(): T? = elementRef.get()
    override fun getContainingFile(): PsiFile? = containingFileRef.get()
    override fun getVirtualFile(): VirtualFile? = containingFile?.virtualFile

    override fun getProject(): Project = project

    override fun getPsiRange(): Segment? = throw UnsupportedOperationException("Not supported")
    override fun getRange(): Segment? = throw UnsupportedOperationException("Not supported")
}

@KaImplementationDetail
@Deprecated("Use 'KaPsiBasedSymbolPointer' instead", ReplaceWith("KaPsiBasedSymbolPointer"))
public typealias KtPsiBasedSymbolPointer<S> = KaPsiBasedSymbolPointer<S>

@OptIn(KaImplementationDetail::class)
@KaExperimentalApi
public fun KtElement.symbolPointer(): KaSymbolPointer<KaSymbol> = KaPsiBasedSymbolPointer(this, KaSymbol::class)

@OptIn(KaImplementationDetail::class)
@KaExperimentalApi
public inline fun <reified S : KaSymbol> KtElement.symbolPointerOfType(): KaSymbolPointer<S> = KaPsiBasedSymbolPointer(this, S::class)
