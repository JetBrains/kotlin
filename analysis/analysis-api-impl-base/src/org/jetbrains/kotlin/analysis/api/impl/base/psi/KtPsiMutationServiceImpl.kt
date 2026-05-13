/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.lexer.KtTokens.COLON
import org.jetbrains.kotlin.psi.EditCommaSeparatedListHelper
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

@KtNonPublicApi
internal class KtPsiMutationServiceImpl : KtPsiMutationService {
    override fun addSuperType(
        declaration: KtClassOrObject,
        superTypeListEntry: KtSuperTypeListEntry,
    ): KtSuperTypeListEntry {
        declaration.getSuperTypeList()?.let { superTypeList ->
            val single = superTypeList.entries.singleOrNull()
            if (single != null && single.typeReference?.typeElement == null) {
                return single.replace(superTypeListEntry) as KtSuperTypeListEntry
            }

            return EditCommaSeparatedListHelper.addItem(superTypeList, declaration.superTypeListEntries, superTypeListEntry)
        }

        val psiFactory = KtPsiFactory(declaration.project)
        val specifierListToAdd = psiFactory.createSuperTypeCallEntry("A()").replace(superTypeListEntry).parent
        val colon = declaration.addBefore(psiFactory.createColon(), declaration.getBody())
        return (declaration.addAfter(specifierListToAdd, colon) as KtSuperTypeList).entries.first()
    }

    override fun addSuperType(
        superTypeList: KtSuperTypeList,
        superTypeListEntry: KtSuperTypeListEntry,
    ): KtSuperTypeListEntry {
        return EditCommaSeparatedListHelper.addItem(superTypeList, superTypeList.entries, superTypeListEntry)
    }

    override fun removeSuperType(declaration: KtClassOrObject, superTypeListEntry: KtSuperTypeListEntry) {
        val specifierList = declaration.getSuperTypeList() ?: return
        assert(superTypeListEntry.parent === specifierList)

        if (specifierList.entries.size > 1) {
            EditCommaSeparatedListHelper.removeItem<KtElement>(superTypeListEntry)
        } else {
            declaration.deleteChildRange(declaration.getColon() ?: specifierList, specifierList)
        }
    }

    override fun removeSuperType(superTypeList: KtSuperTypeList, superTypeListEntry: KtSuperTypeListEntry) {
        EditCommaSeparatedListHelper.removeItem<KtElement>(superTypeListEntry)
        if (superTypeList.entries.isEmpty()) {
            deleteSuperTypeList(superTypeList)
        }
    }

    override fun deleteSuperTypeList(superTypeList: KtSuperTypeList) {
        var left = PsiTreeUtil.skipSiblingsBackward(superTypeList, PsiWhiteSpace::class.java, PsiComment::class.java)
        if (left?.elementType != COLON) {
            left = superTypeList
        }
        superTypeList.parent.deleteChildRange(left, superTypeList)
    }
}
