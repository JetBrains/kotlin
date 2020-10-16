/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceImportAlias.KotlinIntroduceImportAliasHandler
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective

class ReplaceWithImportAliasIntention : SelfTargetingOffsetIndependentIntention<KtNameReferenceExpression>(
    KtNameReferenceExpression::class.java,
    KotlinBundle.lazyMessage("replace.with.import.alias")
) {
    override fun isApplicableTo(element: KtNameReferenceExpression): Boolean {
        if (element.isInImportDirective() || element.getQualifiedElement() !is KtDotQualifiedExpression) return false
        val fqName = element.fqName() ?: return false
        val aliasName = element.containingKtFile.aliasName(fqName) ?: return false
        setTextGetter(KotlinBundle.lazyMessage("replace.with.0", aliasName))
        return true
    }

    override fun applyTo(element: KtNameReferenceExpression, editor: Editor?) {
        val fqName = element.fqName() ?: return
        val file = element.containingKtFile
        val aliasName = file.aliasName(fqName) ?: return
        KotlinIntroduceImportAliasHandler.replaceUsages(file, fqName, aliasName)
    }

    private fun KtNameReferenceExpression.fqName(): FqName? {
        return resolveMainReferenceToDescriptors().firstOrNull()?.importableFqName
    }

    private fun KtFile.aliasName(fqName: FqName): String? {
        val importDirectives = importDirectives.filter { it.importedFqName == fqName }
        if (importDirectives.isEmpty() || importDirectives.any { it.alias == null }) return null
        return importDirectives.firstOrNull()?.aliasName
    }
}