/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class AddDefaultConstructorFix(expectClass: KtClass) : KotlinQuickFixAction<KtClass>(expectClass) {

    override fun getText() = "Add default constructor to expect class"

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

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtClass>? {
            val argumentList = diagnostic.psiElement as? KtValueArgumentList ?: return null
            if (argumentList.arguments.isNotEmpty()) return null
            val derivedClass = argumentList.getStrictParentOfType<KtClassOrObject>() ?: return null
            val context = derivedClass.analyze()
            val baseTypeCallEntry = derivedClass.superTypeListEntries.asSequence().filterIsInstance<KtSuperTypeCallEntry>().firstOrNull()
                ?: return null
            val baseClass = superTypeEntryToClass(baseTypeCallEntry, context) ?: return null
            return AddDefaultConstructorFix(baseClass)
        }
    }
}