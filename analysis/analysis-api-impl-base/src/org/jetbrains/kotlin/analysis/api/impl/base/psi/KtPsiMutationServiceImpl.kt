/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.lexer.KtTokens.COLON
import org.jetbrains.kotlin.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.psi.EditCommaSeparatedListHelper
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

@KtNonPublicApi
internal class KtPsiMutationServiceImpl : KtPsiMutationService {
    override fun deleteElement(element: KtElement) {
        deleteTrailingSemicolon(element)
        element.rawDelete()
    }

    override fun deleteBlockExpression(blockExpression: KtBlockExpression) {
        deleteElement(blockExpression)
    }

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
        val left = PsiTreeUtil.skipSiblingsBackward(superTypeList, PsiWhiteSpace::class.java, PsiComment::class.java)
        if (left?.elementType != COLON) {
            superTypeList.rawDelete()
        } else {
            superTypeList.parent.deleteChildRange(left, superTypeList)
        }
    }

    override fun deleteClassOrObject(declaration: KtClassOrObject) {
        if (declaration is KtEnumEntry) {
            deleteEnumEntry(declaration)
        } else {
            doDeleteClassOrObject(declaration)
        }
    }

    override fun deleteEnumEntry(enumEntry: KtEnumEntry) {
        moveSemicolonBeforeEnumEntryDeletion(enumEntry)
        doDeleteClassOrObject(enumEntry)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KtDeclaration> addMemberDeclaration(classOrObject: KtClassOrObject, declaration: T): T {
        ensureSemicolonIsPresentAfterEnumEntriesIfNecessaryForDeclaration(classOrObject, declaration)
        val body = getOrCreateClassBody(classOrObject)
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.lastChild!!, PsiWhiteSpace::class.java)
        return if (anchor?.nextSibling is PsiErrorElement) {
            body.addBefore(declaration, anchor)
        } else {
            body.addAfter(declaration, anchor)
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KtDeclaration> addMemberDeclarationAfter(
        classOrObject: KtClassOrObject,
        declaration: T,
        anchor: PsiElement?,
    ): T {
        val anchorBefore = anchor ?: classOrObject.declarations.lastOrNull() ?: return addMemberDeclaration(classOrObject, declaration)
        ensureSemicolonIsPresentAfterEnumEntriesIfNecessaryForDeclaration(classOrObject, declaration)
        return getOrCreateClassBody(classOrObject).addAfter(declaration, anchorBefore) as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : KtDeclaration> addMemberDeclarationBefore(
        classOrObject: KtClassOrObject,
        declaration: T,
        anchor: PsiElement?,
    ): T {
        val anchorAfter = anchor ?: classOrObject.declarations.firstOrNull() ?: return addMemberDeclaration(classOrObject, declaration)
        ensureSemicolonIsPresentAfterEnumEntriesIfNecessaryForDeclaration(classOrObject, declaration)
        return getOrCreateClassBody(classOrObject).addBefore(declaration, anchorAfter) as T
    }

    override fun getOrCreateClassBody(classOrObject: KtClassOrObject): KtClassBody {
        classOrObject.getBody()?.let { return it }

        val newBody = KtPsiFactory(classOrObject.project).createEmptyClassBody()
        return if (classOrObject is KtEnumEntry) {
            classOrObject.addAfter(newBody, classOrObject.initializerList ?: classOrObject.nameIdentifier) as KtClassBody
        } else {
            classOrObject.add(newBody) as KtClassBody
        }
    }

    override fun addEnumEntrySemicolon(enumEntry: KtEnumEntry): PsiElement {
        enumEntry.semicolon?.let {
            return it
        }

        // When adding a declaration to an enum class body, there is a chance the next
        // non-whitespace sibling is a semicolon; we should embed it into ourselves.
        val tailStart = enumEntry.nextSibling
        val tailEnd = PsiTreeUtil.skipSiblingsForward(enumEntry, PsiWhiteSpace::class.java, PsiComment::class.java)
        if (tailEnd?.elementType == SEMICOLON) {
            var element = enumEntry.addRangeAfter(tailStart, tailEnd, enumEntry.lastChild)
            enumEntry.parent.deleteChildRange(tailStart, tailEnd)

            while (element.nextSibling != null) {
                element = element.nextSibling
            }

            return element
        }

        val semicolon = KtPsiFactory(enumEntry.project).createSemicolon()
        enumEntry.comma?.let {
            return it.replace(semicolon)
        }
        return enumEntry.addAfter(semicolon, enumEntry.lastChild)
    }

    private fun doDeleteClassOrObject(declaration: KtClassOrObject) {
        CheckUtil.checkWritable(declaration)

        val file = declaration.containingKtFile
        if (!declaration.isTopLevel() || file.declarations.size > 1) {
            deleteElement(declaration)
        } else {
            file.delete()
        }
    }

    private fun moveSemicolonBeforeEnumEntryDeletion(enumEntry: KtEnumEntry) {
        val semicolon = enumEntry.semicolon ?: return

        // Get previous KtEnumEntry, and move semicolon to it.
        val prevEntry = PsiTreeUtil.getPrevSiblingOfType(enumEntry, KtEnumEntry::class.java)
        if (prevEntry == null) {
            // If there is no previous KtEnumEntry, we embed it into the parent.
            val parent = enumEntry.parent
            check(parent is KtClassBody) { "Enum entry should be a child of KtClassBody" }
            parent.addAfter(semicolon, enumEntry)
        } else {
            addEnumEntrySemicolon(prevEntry)
        }
    }

    private fun ensureSemicolonIsPresentAfterEnumEntriesIfNecessaryForDeclaration(
        classOrObject: KtClassOrObject,
        declaration: KtDeclaration,
    ) {
        if (declaration is KtEnumEntry) return
        if (classOrObject !is KtClass || !classOrObject.isEnum()) return

        val body = getOrCreateClassBody(classOrObject)
        val lastEnumEntry = body.children.filterIsInstance<KtEnumEntry>().lastOrNull()

        if (lastEnumEntry != null) {
            addEnumEntrySemicolon(lastEnumEntry)
        } else {
            val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.lastChild!!, PsiWhiteSpace::class.java)
            if (anchor != null && anchor.elementType == SEMICOLON) {
                return
            }
            val psiFactory = KtPsiFactory(classOrObject.project)
            val semicolon = body.addAfter(psiFactory.createSemicolon(), anchor)
            if (anchor == body.lBrace) {
                body.addBefore(psiFactory.createNewLine(), semicolon)
            }
        }
    }

    private fun deleteTrailingSemicolon(element: KtElement) {
        if (element is KtEnumEntry) return

        val sibling = PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java, PsiComment::class.java)
        if (sibling?.elementType != SEMICOLON) return

        val lastSiblingToDelete = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace::class.java)?.prevSibling ?: sibling
        element.parent.deleteChildRange(element.nextSibling, lastSiblingToDelete)
    }

    override fun getOrCreatePrimaryConstructor(klass: KtClass): KtPrimaryConstructor {
        klass.primaryConstructor?.let { return it }

        var anchor: PsiElement? = klass.typeParameterList
        if (anchor == null) anchor = klass.nameIdentifier
        if (anchor == null) anchor = klass.lastChild
        return klass.addAfter(KtPsiFactory(klass.project).createPrimaryConstructor(), anchor) as KtPrimaryConstructor
    }

    override fun getOrCreatePrimaryConstructorParameterList(klass: KtClass): KtParameterList {
        val constructor = getOrCreatePrimaryConstructor(klass)
        constructor.valueParameterList?.let { return it }
        return constructor.add(KtPsiFactory(klass.project).createParameterList("()")) as KtParameterList
    }
}
