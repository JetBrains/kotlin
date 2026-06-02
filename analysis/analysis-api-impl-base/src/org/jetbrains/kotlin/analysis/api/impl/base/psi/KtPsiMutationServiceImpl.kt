/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.psi

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.impl.file.PsiFileImplUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.FileContentUtilCore
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ACTUAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.COLON
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.EXPECT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FINAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.LT
import org.jetbrains.kotlin.lexer.KtTokens.MODIFIER_KEYWORDS_ARRAY
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPERATOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PROTECTED_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.EditCommaSeparatedListHelper
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCommonFile
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContextParameterList
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtFunctionTypeReceiver
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
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
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.createExpressionByPattern
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
                removeModifierKeyword(declaration, OPERATOR_KEYWORD)
            }
        }

        val newIdentifier = KtPsiFactory(declaration.project).createNameIdentifierIfPossible(name.quoteIfNeeded())
        if (newIdentifier != null) {
            astReplace(identifier, newIdentifier)
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

    override fun setModifierList(owner: KtModifierListOwner, newModifierList: KtModifierList) {
        val currentModifierList = owner.modifierList
        if (currentModifierList != null) {
            currentModifierList.replace(newModifierList)
        } else {
            owner.addModifierList(newModifierList)
        }
    }

    override fun replaceModifierList(owner: KtModifierListOwner, modifierList: KtModifierList?): KtModifierList? {
        val oldModifierList = owner.modifierList
        if (modifierList == null) {
            oldModifierList?.delete()
            return null
        } else {
            return if (oldModifierList == null) {
                val firstChild = owner.firstChild
                owner.addBefore(modifierList, firstChild) as KtModifierList
            } else {
                oldModifierList.replace(modifierList) as KtModifierList
            }
        }
    }

    override fun addModifierKeyword(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
        val modifierList = owner.modifierList
        if (modifierList == null) {
            createModifierList(modifier.value, owner)
        } else {
            addModifier(modifierList, modifier)
        }
    }

    override fun addModifierKeyword(constructor: KtPrimaryConstructor, modifier: KtModifierKeywordToken) {
        val modifierList = constructor.modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (constructor.modifierList == null) {
                constructor.getConstructorKeyword()?.delete()
            }
        } else {
            if (modifier == PUBLIC_KEYWORD) return
            val newModifierList = KtPsiFactory(constructor.project).createModifierList(modifier)
            constructor.addBefore(newModifierList, getOrCreateConstructorKeyword(constructor))
        }
    }

    override fun removeModifierKeyword(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
        doRemoveModifier(owner, modifier)
    }

    override fun removeModifierKeyword(constructor: KtPrimaryConstructor, modifier: KtModifierKeywordToken) {
        doRemoveModifier(constructor, modifier)
        if (constructor.modifierList == null) {
            removeRedundantConstructorKeyword(constructor)
        }
    }

    override fun addAnnotation(owner: KtModifierListOwner, annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
        val modifierList = owner.modifierList
        return if (modifierList == null) {
            createModifierList(annotationEntry.text, owner).annotationEntries.first()
        } else {
            modifierList.addBefore(annotationEntry, modifierList.firstChild) as KtAnnotationEntry
        }
    }

    override fun addAnnotation(constructor: KtPrimaryConstructor, annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
        val modifierList = constructor.modifierList
        return if (modifierList != null) {
            modifierList.addBefore(annotationEntry, modifierList.firstChild) as KtAnnotationEntry
        } else {
            val newModifierList = KtPsiFactory(constructor.project).createModifierList(annotationEntry.text)
            val addedModifierList = constructor.addBefore(newModifierList, getOrCreateConstructorKeyword(constructor)) as KtModifierList
            addedModifierList.annotationEntries.first()
        }
    }

    override fun removeAnnotationEntry(annotation: KtAnnotation, entry: KtAnnotationEntry) {
        if (annotation.entries.size > 1) {
            entry.delete()
        } else {
            annotation.delete()
        }
    }

    override fun removeRedundantConstructorKeyword(constructor: KtPrimaryConstructor) {
        constructor.getConstructorKeyword()?.delete()
        if (constructor.prevSibling is PsiWhiteSpace) {
            constructor.prevSibling.delete()
        }
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

    override fun setPropertyInitializer(property: KtProperty, initializer: KtExpression?): KtExpression? {
        val oldInitializer = property.initializer

        if (oldInitializer != null) {
            return if (initializer != null) {
                oldInitializer.replace(initializer) as KtExpression
            } else {
                val nextSibling = oldInitializer.nextSibling
                val last = if (nextSibling?.node?.elementType == SEMICOLON) nextSibling else oldInitializer
                property.deleteChildRange(property.equalsToken, last)
                null
            }
        }

        return if (initializer != null) {
            val addAfter = property.typeReference ?: property.nameIdentifier
            val eq = property.addAfter(KtPsiFactory(property.project).createEQ(), addAfter)
            property.addAfter(initializer, eq) as KtExpression
        } else {
            null
        }
    }

    override fun convertImplicitDelegationCallToExplicit(
        constructor: KtSecondaryConstructor,
        isThis: Boolean,
    ): KtConstructorDelegationCall = with(constructor) {
        val psiFactory = KtPsiFactory(project)
        val current = getDelegationCall()

        assert(current.isImplicit) { "Method should not be called with explicit delegation call: $text" }
        current.delete()

        val colon = addAfter(psiFactory.createColon(), valueParameterList)
        val delegationName = if (isThis) "this" else "super"

        addAfter(psiFactory.creareDelegatedSuperTypeEntry("$delegationName()"), colon) as KtConstructorDelegationCall
    }

    override fun appendParameter(parameterList: KtParameterList, parameter: KtParameter): KtParameter {
        return EditCommaSeparatedListHelper.addItem(parameterList, parameterList.parameters, parameter)
    }

    override fun insertParameterBefore(parameterList: KtParameterList, parameter: KtParameter, anchor: KtParameter?): KtParameter {
        return EditCommaSeparatedListHelper.addItemBefore(parameterList, parameterList.parameters, parameter, anchor)
    }

    override fun insertParameterAfter(parameterList: KtParameterList, parameter: KtParameter, anchor: KtParameter?): KtParameter {
        return EditCommaSeparatedListHelper.addItemAfter(parameterList, parameterList.parameters, parameter, anchor)
    }

    override fun appendTypeParameter(typeParameterList: KtTypeParameterList, typeParameter: KtTypeParameter): KtTypeParameter {
        return EditCommaSeparatedListHelper.addItem(typeParameterList, typeParameterList.parameters, typeParameter, LT)
    }

    override fun appendTypeArgument(typeArgumentList: KtTypeArgumentList, typeArgument: KtTypeProjection): KtTypeProjection {
        return EditCommaSeparatedListHelper.addItem(typeArgumentList, typeArgumentList.arguments, typeArgument, LT)
    }

    override fun appendValueArgument(argumentList: KtValueArgumentList, argument: KtValueArgument): KtValueArgument {
        return EditCommaSeparatedListHelper.addItem(argumentList, argumentList.arguments, argument)
    }

    override fun insertValueArgumentAfter(
        argumentList: KtValueArgumentList,
        argument: KtValueArgument,
        anchor: KtValueArgument?,
    ): KtValueArgument {
        return EditCommaSeparatedListHelper.addItemAfter(argumentList, argumentList.arguments, argument, anchor)
    }

    override fun insertValueArgumentBefore(
        argumentList: KtValueArgumentList,
        argument: KtValueArgument,
        anchor: KtValueArgument?,
    ): KtValueArgument {
        return EditCommaSeparatedListHelper.addItemBefore(argumentList, argumentList.arguments, argument, anchor)
    }

    override fun deleteValueArgument(argumentList: KtValueArgumentList, argument: KtValueArgument) {
        assert(argument.parent == argumentList)
        EditCommaSeparatedListHelper.removeItem(argument)
    }

    override fun deleteValueArgument(argumentList: KtValueArgumentList, index: Int) {
        deleteValueArgument(argumentList, argumentList.arguments[index])
    }

    override fun deleteParameter(parameterList: KtParameterList, parameter: KtParameter) {
        EditCommaSeparatedListHelper.removeItem<KtElement>(parameter)
    }

    override fun deleteParameter(parameterList: KtParameterList, index: Int) {
        deleteParameter(parameterList, parameterList.parameters[index])
    }

    override fun getOrCreateFunctionLiteralParameterList(functionLiteral: KtFunctionLiteral): KtParameterList {
        functionLiteral.valueParameterList?.let { return it }

        val psiFactory = KtPsiFactory(functionLiteral.project)
        val newParameterList =
            functionLiteral.addAfter(psiFactory.createLambdaParameterList("x"), functionLiteral.lBrace) as KtParameterList
        deleteParameter(newParameterList, 0)
        if (functionLiteral.arrow == null) {
            val whitespaceAndArrow = psiFactory.createWhitespaceAndArrow()
            functionLiteral.addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, newParameterList)
        }
        return newParameterList
    }

    override fun getOrCreateCallValueArgumentList(callExpression: KtCallExpression): KtValueArgumentList {
        callExpression.valueArgumentList?.let { return it }

        return callExpression.addAfter(
            KtPsiFactory(callExpression.project).createCallArguments("()"),
            callExpression.typeArgumentList ?: callExpression.calleeExpression,
        ) as KtValueArgumentList
    }

    override fun appendTypeArgument(callExpression: KtCallExpression, typeArgument: KtTypeProjection) {
        callExpression.typeArgumentList?.let {
            appendTypeArgument(it, typeArgument)
            return
        }

        callExpression.addAfter(
            KtPsiFactory(callExpression.project).createTypeArguments("<${typeArgument.text}>"),
            callExpression.calleeExpression,
        )
    }

    override fun astReplace(element: PsiElement, newElement: PsiElement) {
        element.parent.node.replaceChild(element.node, newElement.node)
    }

    override fun replaceExpression(
        expression: KtExpression,
        newElement: PsiElement,
        reformat: Boolean,
        rawReplaceHandler: (PsiElement) -> PsiElement,
    ): PsiElement {
        val parent = expression.parent

        if (newElement is KtExpression) {
            when (parent) {
                is KtExpression, is KtValueArgument -> {
                    if (KtPsiUtil.areParenthesesNecessary(newElement, expression, parent)) {
                        val factory = KtPsiFactory(expression.project)
                        return rawReplaceHandler(factory.createExpressionByPattern("($0)", newElement, reformat = reformat))
                    }
                }
                is KtSimpleNameStringTemplateEntry -> {
                    if (newElement !is KtSimpleNameExpression && !newElement.isThisWithoutLabel()) {
                        val factory = KtPsiFactory(expression.project)
                        val newEntry = parent.replace(factory.createBlockStringTemplateEntry(newElement)) as KtBlockStringTemplateEntry
                        return newEntry.expression!!
                    }
                }
            }
        }

        return rawReplaceHandler(newElement)
    }

    override fun updateStringTemplateText(expression: KtStringTemplateExpression, text: String): PsiLanguageInjectionHost {
        val newExpression = KtPsiFactory(expression.project).createExpressionIfPossible(text)
        if (newExpression is KtStringTemplateExpression) {
            return expression.replace(newExpression) as KtStringTemplateExpression
        }

        return ElementManipulators.handleContentChange(expression, text)
    }

    override fun updateKDocSectionText(section: KDocSection, text: String): PsiLanguageInjectionHost {
        val comment = KtPsiFactory(section.project).createComment("/**\n$text\n*/")
        val snippet = PsiTreeUtil.findChildOfType(comment, KDocSection::class.java)
        return snippet ?: section
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

    private fun PsiElement.isThisWithoutLabel(): Boolean = this is KtThisExpression && getLabelName() == null

    private fun getOrCreateConstructorKeyword(constructor: KtPrimaryConstructor): PsiElement {
        return constructor.getConstructorKeyword()
            ?: constructor.addBefore(KtPsiFactory(constructor.project).createConstructorKeyword(), constructor.valueParameterList!!)
    }

    private fun KtModifierListOwner.addModifierList(newModifierList: KtModifierList): KtModifierList {
        val anchor = firstChild!!
            .siblings(forward = true)
            .dropWhile { it is PsiComment || it is PsiWhiteSpace || it is KtContextParameterList }
            .first()
        return addBefore(newModifierList, anchor) as KtModifierList
    }

    private fun createModifierList(text: String, owner: KtModifierListOwner): KtModifierList {
        return owner.addModifierList(KtPsiFactory(owner.project).createModifierList(text))
    }

    private fun addModifier(modifierList: KtModifierList, modifier: KtModifierKeywordToken) {
        if (modifierList.hasModifier(modifier)) return

        val newModifier = KtPsiFactory(modifierList.project).createModifier(modifier)
        val modifierToReplace = MODIFIERS_TO_REPLACE[modifier]
            ?.firstNotNullOfOrNull(modifierList::getModifier)

        if (modifier == FINAL_KEYWORD && !modifierList.hasModifier(OVERRIDE_KEYWORD)) {
            if (modifierToReplace != null) {
                modifierToReplace.delete()
                if (modifierList.firstChild == null) {
                    modifierList.delete()
                }
            }
            return
        }

        if (modifierToReplace != null && modifierList.firstChild == modifierList.lastChild) {
            modifierToReplace.replace(newModifier)
        } else {
            modifierToReplace?.delete()
            val newModifierOrder = MODIFIER_KEYWORDS_ARRAY.indexOf(modifier)

            fun placeAfter(child: PsiElement): Boolean {
                if (child is PsiWhiteSpace) return false
                if (child is KtAnnotation || child is KtAnnotationEntry) return true
                val order = MODIFIER_KEYWORDS_ARRAY.indexOf(child.node.elementType)
                return newModifierOrder > order
            }

            val lastChild = modifierList.lastChild
            val anchor = lastChild?.siblings(forward = false)?.firstOrNull(::placeAfter).let {
                when {
                    it?.nextSibling is PsiWhiteSpace &&
                            (it is KtAnnotation || it is KtAnnotationEntry || it is KtContextParameterList || it is PsiComment) -> it.nextSibling
                    it == null && modifierList.firstChild is PsiWhiteSpace -> modifierList.firstChild
                    else -> it
                }
            }
            modifierList.addAfter(newModifier, anchor)

            if (anchor == lastChild) {
                val whiteSpace = modifierList.nextSibling as? PsiWhiteSpace
                if (whiteSpace != null && whiteSpace.text.contains('\n')) {
                    modifierList.addAfter(whiteSpace, anchor)
                    whiteSpace.delete()
                }
            }
        }
    }

    private fun doRemoveModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
        val modifierList = owner.modifierList ?: return
        val modifierElement = modifierList.getModifier(modifier)
        if (modifierElement != null) {
            val forward = modifierList.lastChild != modifierElement
            val rangeEnd = modifierElement.siblings(forward = forward, withItself = true)
                .takeWhile { it is PsiWhiteSpace || it == modifierElement }
                .last()

            if (forward) {
                modifierList.deleteChildRange(modifierElement, rangeEnd)
            } else {
                modifierList.deleteChildRange(rangeEnd, modifierElement)
            }
        }

        if (modifierList.firstChild == null) {
            val rangeEnd = modifierList.siblings(forward = true, withItself = true)
                .takeWhile { it is PsiWhiteSpace || it == modifierList }
                .last()
            owner.deleteChildRange(modifierList, rangeEnd)
            return
        }

        val lastChild = modifierList.lastChild
        if (lastChild is PsiComment) {
            modifierList.addAfter(KtPsiFactory(owner.project).createNewLine(), lastChild)
        }
    }

    private companion object {
        val FUNCTIONLIKE_CONVENTIONS = setOf(
            OperatorNameConventions.INVOKE.asString(),
            OperatorNameConventions.GET.asString(),
        )

        val MODIFIERS_TO_REPLACE = mapOf(
            OVERRIDE_KEYWORD to listOf(OPEN_KEYWORD),
            ABSTRACT_KEYWORD to listOf(OPEN_KEYWORD, FINAL_KEYWORD),
            OPEN_KEYWORD to listOf(FINAL_KEYWORD, ABSTRACT_KEYWORD),
            FINAL_KEYWORD to listOf(ABSTRACT_KEYWORD, OPEN_KEYWORD),
            PUBLIC_KEYWORD to listOf(PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
            PROTECTED_KEYWORD to listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
            PRIVATE_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, INTERNAL_KEYWORD),
            INTERNAL_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD),
            EXPECT_KEYWORD to listOf(ACTUAL_KEYWORD),
            ACTUAL_KEYWORD to listOf(EXPECT_KEYWORD),
        )
    }

    private fun shouldDropOperatorKeyword(oldName: String?, newName: String): Boolean {
        return !OperatorTokens.isConventionName(Name.identifier(newName)) ||
                FUNCTIONLIKE_CONVENTIONS.contains(oldName) != FUNCTIONLIKE_CONVENTIONS.contains(newName)
    }
}
