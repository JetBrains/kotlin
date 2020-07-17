/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class AddDefaultConstructorFix(expectClass: KtClass) : KotlinQuickFixAction<KtClass>(expectClass) {

    override fun getText() = KotlinBundle.message("fix.add.default.constructor")

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.createPrimaryConstructorIfAbsent()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        fun superTypeEntryToClass(typeEntry: KtSuperTypeListEntry, context: BindingContext): KtClass? {
            val baseType = context[BindingContext.TYPE, typeEntry.typeReference] ?: return null
            val baseClassDescriptor = baseType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            if (!baseClassDescriptor.isExpect) return null
            if (baseClassDescriptor.kind != ClassKind.CLASS) return null
            return DescriptorToSourceUtils.descriptorToDeclaration(baseClassDescriptor) as? KtClass
        }

        private fun annotationEntryToClass(entry: KtAnnotationEntry, context: BindingContext): KtClass? {
            val descriptor =
                context[BindingContext.ANNOTATION, entry]?.type?.constructor?.declarationDescriptor as? ClassDescriptor ?: return null
            if (!descriptor.isExpect) return null
            return DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtClass
        }

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtClass>? {
            val element = diagnostic.psiElement
            if (element is KtValueArgumentList && element.arguments.isNotEmpty()) return null
            val parent = element.getParentOfTypes(true, KtClassOrObject::class.java, KtAnnotationEntry::class.java) ?: return null
            val context by lazy { parent.analyze() }
            val baseClass = when (parent) {
                is KtClassOrObject -> parent.superTypeListEntries.asSequence().filterIsInstance<KtSuperTypeCallEntry>().firstOrNull()?.let {
                    superTypeEntryToClass(it, context)
                }
                is KtAnnotationEntry -> annotationEntryToClass(parent, context)
                else -> null
            } ?: return null
            return AddDefaultConstructorFix(baseClass)
        }
    }
}