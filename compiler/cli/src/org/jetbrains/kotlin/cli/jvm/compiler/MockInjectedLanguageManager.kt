/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost

class MockInjectedLanguageManager : InjectedLanguageManager() {
    override fun mightHaveInjectedFragmentAtOffset(hostDocument: Document, hostOffset: Int): Boolean = false

    override fun getTopLevelFile(element: PsiElement): PsiFile? = null
    override fun getUnescapedText(injectedNode: PsiElement): String = ""
    override fun getInjectionHost(injectedProvider: FileViewProvider): PsiLanguageInjectionHost? = null
    override fun getInjectionHost(injectedElement: PsiElement): PsiLanguageInjectionHost? = null
    override fun getInjectedPsiFiles(host: PsiElement): List<Pair<PsiElement, TextRange>> = emptyList()

    override fun enumerate(host: PsiElement, visitor: PsiLanguageInjectionHost.InjectedPsiVisitor) {}
    override fun enumerateEx(
        host: PsiElement, containingFile: PsiFile, probeUp: Boolean, visitor: PsiLanguageInjectionHost.InjectedPsiVisitor) {}

    override fun getCachedInjectedDocumentsInRange(hostPsiFile: PsiFile, range: TextRange): List<DocumentWindow> = emptyList()

    override fun dropFileCaches(file: PsiFile) {}
    override fun getNonEditableFragments(window: DocumentWindow): List<TextRange> = emptyList()

    override fun registerMultiHostInjector(injector: MultiHostInjector) {}
    override fun registerMultiHostInjector(injector: MultiHostInjector, parentDisposable: Disposable) {}

    override fun isInjectedFragment(injectedFile: PsiFile): Boolean = false

    override fun intersectWithAllEditableFragments(injectedPsi: PsiFile, rangeToEdit: TextRange): List<TextRange> = emptyList()

    override fun freezeWindow(document: DocumentWindow): DocumentWindow = document
    override fun findInjectedElementAt(hostFile: PsiFile, hostDocumentOffset: Int): PsiElement? = null

    override fun injectedToHost(injectedContext: PsiElement, injectedTextRange: TextRange): TextRange = injectedTextRange
    override fun injectedToHost(injectedContext: PsiElement, injectedOffset: Int): Int = injectedOffset

    // BUNCH: 183
    @Suppress("MissingRecentApi")
    override fun injectedToHost(injectedContext: PsiElement, injectedOffset: Int, minHostOffset: Boolean): Int = injectedOffset
}