/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.presentation

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProvider
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinIconsIndependent
import org.jetbrains.kotlin.psi.*
import javax.swing.Icon

class KtJvmNameAnnotationPresenter : ItemPresentationProvider<KtAnnotationEntry> {
    override fun getPresentation(annotationEntry: KtAnnotationEntry): ItemPresentation? {
        if (annotationEntry.shortName?.asString() != JvmFileClassUtil.JVM_NAME_SHORT) return null

        return when (val grandParent = annotationEntry.parent.parent) {
            is KtFile -> KtJvmNameAnnotatedFilePresentation(annotationEntry)
            is KtFunction -> KotlinFunctionPresentation(grandParent, JvmFileClassUtil.getLiteralStringFromAnnotation(annotationEntry))
            is KtNamedDeclaration -> getDeclarationPresentation(grandParent, annotationEntry)
            is KtPropertyAccessor -> {
                val property = grandParent.parentOfType<KtProperty>() ?: return null
                getDeclarationPresentation(property, annotationEntry)
            }
            else -> null
        }
    }

    private fun getDeclarationPresentation(declaration: KtNamedDeclaration, annotationEntry: KtAnnotationEntry): ItemPresentation {
        return object : KotlinDefaultNamedDeclarationPresentation(declaration) {
            override fun getPresentableText(): String? = JvmFileClassUtil.getLiteralStringFromAnnotation(annotationEntry)
        }
    }
}

class KtJvmNameAnnotatedFilePresentation(private val annotationEntry: KtAnnotationEntry) : ItemPresentation {
    private val containingFile = annotationEntry.containingKtFile

    override fun getPresentableText(): String? = JvmFileClassUtil.getLiteralStringFromAnnotation(annotationEntry)

    override fun getLocationString(): String {
        return KotlinBundle.message("presentation.text.in.container", containingFile.name, containingFile.packageFqName)
    }

    override fun getIcon(unused: Boolean): Icon = KotlinIconsIndependent.FILE
}