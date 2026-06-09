/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.java.stubs.PsiAnnotationStub
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.IncorrectOperationException

/**
 * A [PsiAnnotationStub] wrapper that tolerates annotation texts which are not valid Java.
 *
 * Decompiled light classes are built from Kotlin `.class` files via IntelliJ's compiled-class reader, which
 * renders an annotation back into Java-like text. Kotlin permits identifiers that are not valid in Java (for
 * example, an enum entry named `` `3x3` ``), so an annotation argument that references such an entry produces
 * text like `@Sized(value=Size.3x3)`. That text cannot be parsed back into Java PSI, and the default
 * [PsiAnnotationStub.getPsiElement] implementation reports an error in this case (KT-57328).
 *
 * To avoid the failure, [getPsiElement] falls back to the argument-less form of the annotation (`@Sized()`)
 * when the original text is not a valid Java annotation. This drops the unrepresentable arguments, which is
 * consistent with how the metadata-based decompiler renders the same declarations.
 */
internal class SafeClsAnnotationStub(
    private val delegate: PsiAnnotationStub,
) : PsiAnnotationStub by delegate {
    override fun getPsiElement(): PsiAnnotation? {
        // The already-created `ClsAnnotationImpl` is used as the resolution context, mirroring the default
        // `PsiAnnotationStubImpl.getPsiElement()` behavior.
        val context = delegate.psi
        val annotationText = text
        return createAnnotationFromText(annotationText, context)
            ?: createAnnotationFromText(annotationText.substringBefore('(') + "()", context)
    }

    private fun createAnnotationFromText(text: String, context: PsiElement): PsiAnnotation? {
        val annotation = try {
            JavaPsiFacade.getInstance(context.project).parserFacade.createAnnotationFromText(text, context)
        } catch (_: IncorrectOperationException) {
            return null
        }

        // Mark the throwaway light file as read-only, mirroring `PsiAnnotationStubImpl.getPsiElement()`.
        (annotation.containingFile.viewProvider.virtualFile as? LightVirtualFile)?.isWritable = false
        return annotation
    }

    override fun findChildStubByElementType(elementType: IElementType): StubElement<out PsiElement?>? =
        delegate.findChildStubByElementType(elementType)

    override fun getStubSerializer(): ObjectStubSerializer<*, out Stub?>? = delegate.stubSerializer
}
