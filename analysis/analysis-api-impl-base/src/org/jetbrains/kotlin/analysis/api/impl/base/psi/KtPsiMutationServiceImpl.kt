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
import com.intellij.psi.impl.file.PsiFileImplUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.FileContentUtilCore
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KtTokens.COLON
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.OPERATOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.EditCommaSeparatedListHelper
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCommonFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtFunctionTypeReceiver
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclarationStub
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.addRemoveModifier.removeModifier as removeModifierFromPsi
import org.jetbrains.kotlin.psi.psiUtil.astReplace
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.typeRefHelpers.getTypeReference
import org.jetbrains.kotlin.psi.utils.OperatorTokens
import org.jetbrains.kotlin.util.OperatorNameConventions

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

    override fun setNamedDeclarationStubName(declaration: KtNamedDeclarationStub<*>, name: String): PsiElement? {
        val identifier = declaration.nameIdentifier ?: return null

        val modifierList = declaration.modifierList
        if (modifierList != null && modifierList.hasModifier(OPERATOR_KEYWORD)) {
            if (shouldDropOperatorKeyword(declaration.name, name)) {
                removeModifierFromPsi(declaration, OPERATOR_KEYWORD)
            }
        }

        val newIdentifier = KtPsiFactory(declaration.project).createNameIdentifierIfPossible(name.quoteIfNeeded())
        if (newIdentifier != null) {
            identifier.astReplace(newIdentifier)
        } else {
            identifier.delete()
        }
        return declaration
    }

    override fun setNamedDeclarationName(declaration: KtNamedDeclaration, name: String): PsiElement {
        val identifier = declaration.nameIdentifier ?: throw IncorrectOperationException()
        return identifier.replace(KtPsiFactory(declaration.project).createNameIdentifier(name))
    }

    override fun setLabeledExpressionName(expression: KtLabeledExpression, name: String): PsiElement {
        expression.getTargetLabel()?.replace(KtPsiFactory(expression.project).createLabeledExpression(name).getTargetLabel()!!)
        return expression
    }

    override fun setImportAliasName(importAlias: KtImportAlias, name: String): PsiElement {
        importAlias.nameIdentifier?.replace(KtPsiFactory(importAlias.project).createNameIdentifier(name))
        return importAlias
    }

    override fun setObjectDeclarationName(declaration: KtObjectDeclaration, name: String): PsiElement {
        return if (declaration.nameIdentifier == null) {
            val psiFactory = KtPsiFactory(declaration.project)
            val result = declaration.addAfter(psiFactory.createIdentifier(name), declaration.getObjectKeyword()!!)
            declaration.addAfter(psiFactory.createWhiteSpace(), declaration.getObjectKeyword()!!)

            result
        } else {
            setNamedDeclarationStubName(declaration as KtNamedDeclarationStub<*>, name) ?: declaration
        }
    }

    @Suppress("DEPRECATION")
    override fun setCommonFileName(file: KtCommonFile, name: String): PsiElement {
        file.checkSetName(name)
        val result = PsiFileImplUtil.setName(file, name)
        val willBeScript = name.endsWith(KotlinFileType.SCRIPT_EXTENSION)
        if (file.isScript() != willBeScript) {
            FileContentUtilCore.reparseFiles(listOfNotNull(file.virtualFile))
        }
        return result
    }

    @Suppress("DEPRECATION")
    override fun setPackageFqName(file: KtCommonFile, fqName: FqName) {
        val packageDirective = file.packageDirective
        if (packageDirective != null) {
            setPackageFqName(packageDirective, fqName)
            return
        }

        val newPackageDirective = KtPsiFactory(file.project).createPackageDirectiveIfNeeded(fqName) ?: return
        file.addAfter(newPackageDirective, null)
    }

    override fun setPackageFqName(packageDirective: KtPackageDirective, fqName: FqName) {
        if (fqName.isRoot) {
            if (!packageDirective.fqName.isRoot) {
                packageDirective.replace(KtPsiFactory(packageDirective.project).createFile("").packageDirective!!)
            }
            return
        }

        val psiFactory = KtPsiFactory(packageDirective.project)
        val newExpression = psiFactory.createExpression(fqName.asString())
        val currentExpression = packageDirective.packageNameExpression
        if (currentExpression != null) {
            currentExpression.replace(newExpression)
            return
        }

        val keyword = packageDirective.packageKeyword
        if (keyword != null) {
            packageDirective.addAfter(newExpression, keyword)
            packageDirective.addAfter(psiFactory.createWhiteSpace(), keyword)
            return
        }

        packageDirective.replace(psiFactory.createPackageDirective(fqName))
    }

    override fun replaceFileAnnotationList(file: KtFile, annotationList: KtFileAnnotationList): KtFileAnnotationList {
        file.fileAnnotationList?.let {
            return it.replace(annotationList) as KtFileAnnotationList
        }

        val beforeAnchor: PsiElement? = when {
            file.packageDirective?.packageKeyword != null -> file.packageDirective
            file.importList != null -> file.importList
            file.declarations.firstOrNull() != null -> file.declarations.first()
            else -> null
        }

        if (beforeAnchor != null) {
            return file.addBefore(annotationList, beforeAnchor) as KtFileAnnotationList
        }

        if (file.lastChild == null) {
            return file.add(annotationList) as KtFileAnnotationList
        }

        return file.addAfter(annotationList, file.lastChild) as KtFileAnnotationList
    }

    override fun setFunctionTypeReference(function: KtNamedFunction, typeRef: KtTypeReference?): KtTypeReference? {
        return setCallableTypeReference(function, function.valueParameterList, typeRef)
    }

    override fun setPropertyTypeReference(property: KtProperty, typeRef: KtTypeReference?): KtTypeReference? {
        return setCallableTypeReference(property, property.nameIdentifier, typeRef)
    }

    override fun setParameterTypeReference(parameter: KtParameter, typeRef: KtTypeReference?): KtTypeReference? {
        return setCallableTypeReference(parameter, parameter.nameIdentifier, typeRef)
    }

    override fun setDestructuringDeclarationEntryTypeReference(
        entry: KtDestructuringDeclarationEntry,
        typeRef: KtTypeReference?,
    ): KtTypeReference? {
        return setCallableTypeReference(entry, entry.nameIdentifier, typeRef)
    }

    override fun setCallableTypeReference(
        declaration: KtCallableDeclaration,
        addAfter: PsiElement?,
        typeRef: KtTypeReference?,
    ): KtTypeReference? {
        val oldTypeRef = getTypeReference(declaration)
        if (typeRef != null) {
            return if (oldTypeRef != null) {
                oldTypeRef.replace(typeRef) as KtTypeReference
            } else {
                val anchor = addAfter
                    ?: declaration.nameIdentifier?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
                    ?: (declaration as? KtParameter)?.destructuringDeclaration
                val newTypeRef = declaration.addAfter(typeRef, anchor) as KtTypeReference
                declaration.addAfter(KtPsiFactory(declaration.project).createColon(), anchor)
                newTypeRef
            }
        }

        if (oldTypeRef != null) {
            val colon = declaration.colon!!
            val removeFrom = colon.prevSibling as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }

    override fun setCallableReceiverTypeReference(declaration: KtCallableDeclaration, typeRef: KtTypeReference?): KtTypeReference? {
        return declaration.doSetReceiverTypeReference(
            typeRef,
            { receiverTypeReference },
            { addBefore(it, nameIdentifier ?: valueParameterList) as KtTypeReference }
        )
    }

    override fun setFunctionTypeReceiverTypeReference(functionType: KtFunctionType, typeRef: KtTypeReference?): KtTypeReference? {
        return functionType.doSetReceiverTypeReference(
            typeRef,
            { receiverTypeReference },
            {
                (addBefore(
                    KtPsiFactory(project).createFunctionTypeReceiver(it),
                    parameterList ?: firstChild
                ) as KtFunctionTypeReceiver).typeReference
            }
        )
    }

    override fun setTypeParameterExtendsBound(
        typeParameter: KtTypeParameter,
        typeReference: KtTypeReference?,
    ): KtTypeReference? {
        val currentExtendsBound = typeParameter.extendsBound
        if (currentExtendsBound != null) {
            return if (typeReference == null) {
                typeParameter.node.findChildByType(COLON)?.psi?.delete()
                currentExtendsBound.delete()
                null
            } else {
                currentExtendsBound.replace(typeReference) as KtTypeReference
            }
        }

        return if (typeReference != null) {
            val colon = typeParameter.addAfter(KtPsiFactory(typeParameter.project).createColon(), typeParameter.nameIdentifier)
            typeParameter.addAfter(typeReference, colon) as KtTypeReference
        } else {
            null
        }
    }

    override fun setDoubleColonReceiverExpression(expression: KtDoubleColonExpression, newReceiverExpression: KtExpression) {
        val oldReceiverExpression = expression.receiverExpression
        oldReceiverExpression?.replace(newReceiverExpression)
            ?: expression.addBefore(newReceiverExpression, expression.doubleColonTokenReference)
    }

    override fun removeQualifier(userType: KtUserType) {
        val qualifier = userType.qualifier
        assert(qualifier != null)
        val dot = userType.node.findChildByType(DOT)?.psi
        assert(dot != null)
        qualifier!!.delete()
        dot!!.delete()
    }

    private inline fun <T : KtElement> T.doSetReceiverTypeReference(
        typeRef: KtTypeReference?,
        getReceiverTypeReference: T.() -> KtTypeReference?,
        addReceiverTypeReference: T.(typeRef: KtTypeReference) -> KtTypeReference,
    ): KtTypeReference? {
        val needParentheses = typeRef != null && typeRef.typeElement is KtFunctionType && !typeRef.hasParentheses()
        val oldTypeRef = getReceiverTypeReference()
        if (typeRef != null) {
            val newTypeRef = if (oldTypeRef != null) {
                oldTypeRef.replace(typeRef) as KtTypeReference
            } else {
                val newTypeRef = addReceiverTypeReference(typeRef)
                addAfter(KtPsiFactory(project).createDot(), newTypeRef.parentsWithSelf.first { it.parent == this })
                newTypeRef
            }
            if (needParentheses) {
                val argList = KtPsiFactory(project).createCallArguments("()")
                newTypeRef.addBefore(argList.leftParenthesis!!, newTypeRef.firstChild)
                newTypeRef.add(argList.rightParenthesis!!)
            }
            return newTypeRef
        }

        if (oldTypeRef != null) {
            val dotSibling = oldTypeRef.parent as? KtFunctionTypeReceiver ?: oldTypeRef
            val dot = dotSibling.siblings(forward = true).firstOrNull { it.node.elementType == DOT }
            deleteChildRange(dotSibling, dot ?: dotSibling)
        }
        return null
    }

    private companion object {
        val FUNCTIONLIKE_CONVENTIONS = setOf(
            OperatorNameConventions.INVOKE.asString(),
            OperatorNameConventions.GET.asString(),
        )
    }

    private fun shouldDropOperatorKeyword(oldName: String?, newName: String): Boolean {
        return !OperatorTokens.isConventionName(Name.identifier(newName)) ||
                FUNCTIONLIKE_CONVENTIONS.contains(oldName) != FUNCTIONLIKE_CONVENTIONS.contains(newName)
    }
}
