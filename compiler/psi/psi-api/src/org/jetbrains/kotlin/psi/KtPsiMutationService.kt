/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName

/**
 * Service responsible for Kotlin PSI mutation operations whose implementation is provided by the Kotlin plugin environment.
 */
@KtNonPublicApi
interface KtPsiMutationService {
    /**
     * Performs smart deletion of [element].
     */
    fun deleteElement(element: KtElement)

    /**
     * Performs smart deletion of [blockExpression].
     */
    fun deleteBlockExpression(blockExpression: KtBlockExpression)

    /**
     * Adds [superTypeListEntry] to [declaration].
     */
    fun addSuperType(declaration: KtClassOrObject, superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry

    /**
     * Adds [superTypeListEntry] to [superTypeList].
     */
    fun addSuperType(superTypeList: KtSuperTypeList, superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry

    /**
     * Removes [superTypeListEntry] from [declaration].
     */
    fun removeSuperType(declaration: KtClassOrObject, superTypeListEntry: KtSuperTypeListEntry)

    /**
     * Removes [superTypeListEntry] from [superTypeList].
     */
    fun removeSuperType(superTypeList: KtSuperTypeList, superTypeListEntry: KtSuperTypeListEntry)

    /**
     * Deletes [superTypeList], removing the preceding colon when needed.
     */
    fun deleteSuperTypeList(superTypeList: KtSuperTypeList)

    /**
     * Performs smart deletion of [declaration].
     */
    fun deleteClassOrObject(declaration: KtClassOrObject)

    /**
     * Performs smart deletion of [enumEntry].
     */
    fun deleteEnumEntry(enumEntry: KtEnumEntry)

    /**
     * Adds [declaration] to [classOrObject], creating a body when needed.
     */
    fun <T : KtDeclaration> addMemberDeclaration(classOrObject: KtClassOrObject, declaration: T): T

    /**
     * Adds [declaration] after [anchor] in [classOrObject], or appends it when [anchor] is `null`.
     */
    fun <T : KtDeclaration> addMemberDeclarationAfter(classOrObject: KtClassOrObject, declaration: T, anchor: PsiElement?): T

    /**
     * Adds [declaration] before [anchor] in [classOrObject], or prepends it when [anchor] is `null`.
     */
    fun <T : KtDeclaration> addMemberDeclarationBefore(classOrObject: KtClassOrObject, declaration: T, anchor: PsiElement?): T

    /**
     * Returns the existing body for [classOrObject], or creates one if missing.
     */
    fun getOrCreateClassBody(classOrObject: KtClassOrObject): KtClassBody

    /**
     * Adds a semicolon to [enumEntry], reusing an existing sibling semicolon when possible.
     */
    fun addEnumEntrySemicolon(enumEntry: KtEnumEntry): PsiElement

    /**
     * Returns the existing primary constructor for [klass], or creates one if missing.
     */
    fun getOrCreatePrimaryConstructor(klass: KtClass): KtPrimaryConstructor

    /**
     * Returns the existing primary constructor parameter list for [klass], or creates one if missing.
     */
    fun getOrCreatePrimaryConstructorParameterList(klass: KtClass): KtParameterList

    /**
     * Renames [declaration], including operator-specific modifier adjustments.
     */
    fun setNamedDeclarationStubName(declaration: KtNamedDeclarationStub<*>, name: String): PsiElement?

    /**
     * Renames [declaration] by replacing its name identifier directly.
     */
    fun setNamedDeclarationName(declaration: KtNamedDeclaration, name: String): PsiElement

    /**
     * Renames [expression] by replacing its target label.
     */
    fun setLabeledExpressionName(expression: KtLabeledExpression, name: String): PsiElement

    /**
     * Renames [importAlias].
     */
    fun setImportAliasName(importAlias: KtImportAlias, name: String): PsiElement

    /**
     * Renames [declaration], adding an explicit identifier when necessary.
     */
    fun setObjectDeclarationName(declaration: KtObjectDeclaration, name: String): PsiElement

    /**
     * Renames [file], reparsing it when the rename changes whether it should be treated as a script.
     */
    @Suppress("DEPRECATION")
    fun setCommonFileName(file: KtCommonFile, name: String): PsiElement

    /**
     * Replaces the package name of [file], adding a package directive when needed.
     */
    @Suppress("DEPRECATION")
    fun setPackageFqName(file: KtCommonFile, fqName: FqName)

    /**
     * Replaces the package name of [packageDirective] with [fqName].
     */
    fun setPackageFqName(packageDirective: KtPackageDirective, fqName: FqName)

    /**
     * Replaces [file]'s file annotation list with [annotationList], or adds it when missing.
     */
    fun replaceFileAnnotationList(file: KtFile, annotationList: KtFileAnnotationList): KtFileAnnotationList

    /**
     * Replaces the existing modifier list on [owner] with [newModifierList], or adds it if missing.
     */
    fun setModifierList(owner: KtModifierListOwner, newModifierList: KtModifierList)

    /**
     * Replaces the existing modifier list on [owner] with [modifierList], adds it if missing, or removes it when [modifierList] is `null`.
     */
    fun replaceModifierList(owner: KtModifierListOwner, modifierList: KtModifierList?): KtModifierList?

    /**
     * Adds [modifier] to [owner].
     */
    fun addModifierKeyword(owner: KtModifierListOwner, modifier: KtModifierKeywordToken)

    /**
     * Adds [modifier] to [constructor] using primary-constructor-specific behavior.
     */
    fun addModifierKeyword(constructor: KtPrimaryConstructor, modifier: KtModifierKeywordToken)

    /**
     * Removes [modifier] from [owner].
     */
    fun removeModifierKeyword(owner: KtModifierListOwner, modifier: KtModifierKeywordToken)

    /**
     * Removes [modifier] from [constructor] using primary-constructor-specific behavior.
     */
    fun removeModifierKeyword(constructor: KtPrimaryConstructor, modifier: KtModifierKeywordToken)

    /**
     * Adds [annotationEntry] to [owner].
     */
    fun addAnnotation(owner: KtModifierListOwner, annotationEntry: KtAnnotationEntry): KtAnnotationEntry

    /**
     * Adds [annotationEntry] to [constructor] using primary-constructor-specific behavior.
     */
    fun addAnnotation(constructor: KtPrimaryConstructor, annotationEntry: KtAnnotationEntry): KtAnnotationEntry

    /**
     * Removes [entry] from [annotation], deleting the annotation when it becomes empty.
     */
    fun removeAnnotationEntry(annotation: KtAnnotation, entry: KtAnnotationEntry)

    /**
     * Removes the redundant `constructor` keyword and the following whitespace from [constructor].
     */
    fun removeRedundantConstructorKeyword(constructor: KtPrimaryConstructor)

    /**
     * Replaces the type reference on [function] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setFunctionTypeReference(function: KtNamedFunction, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the type reference on [property] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setPropertyTypeReference(property: KtProperty, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the type reference on [parameter] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setParameterTypeReference(parameter: KtParameter, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the type reference on [entry] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setDestructuringDeclarationEntryTypeReference(entry: KtDestructuringDeclarationEntry, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the explicit return type on [declaration] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setCallableTypeReference(declaration: KtCallableDeclaration, addAfter: PsiElement?, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the receiver type on [declaration] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setCallableReceiverTypeReference(declaration: KtCallableDeclaration, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the receiver type on [functionType] with [typeRef], adds it if missing, or removes it when [typeRef] is `null`.
     */
    fun setFunctionTypeReceiverTypeReference(functionType: KtFunctionType, typeRef: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the extends bound on [typeParameter] with [typeReference], adds it if missing, or removes it when [typeReference] is `null`.
     */
    fun setTypeParameterExtendsBound(typeParameter: KtTypeParameter, typeReference: KtTypeReference?): KtTypeReference?

    /**
     * Replaces the receiver expression on [expression] with [newReceiverExpression], or adds it if missing.
     */
    fun setDoubleColonReceiverExpression(expression: KtDoubleColonExpression, newReceiverExpression: KtExpression)

    /**
     * Removes the qualifier of [userType], keeping the referenced name intact.
     */
    fun removeQualifier(userType: KtUserType)

    /**
     * Replaces the initializer on [property] with [initializer], adds it if missing, or removes it when [initializer] is `null`.
     */
    fun setPropertyInitializer(property: KtProperty, initializer: KtExpression?): KtExpression?

    /**
     * Replaces the implicit delegation call in [constructor] with an explicit `this()` or `super()` call.
     */
    fun convertImplicitDelegationCallToExplicit(constructor: KtSecondaryConstructor, isThis: Boolean): KtConstructorDelegationCall

    /**
     * Adds [parameter] to [parameterList].
     */
    fun appendParameter(parameterList: KtParameterList, parameter: KtParameter): KtParameter

    /**
     * Adds [parameter] to [parameterList] before [anchor].
     */
    fun insertParameterBefore(parameterList: KtParameterList, parameter: KtParameter, anchor: KtParameter?): KtParameter

    /**
     * Adds [parameter] to [parameterList] after [anchor].
     */
    fun insertParameterAfter(parameterList: KtParameterList, parameter: KtParameter, anchor: KtParameter?): KtParameter

    /**
     * Adds [typeParameter] to [typeParameterList].
     */
    fun appendTypeParameter(typeParameterList: KtTypeParameterList, typeParameter: KtTypeParameter): KtTypeParameter

    /**
     * Adds [typeArgument] to [typeArgumentList].
     */
    fun appendTypeArgument(typeArgumentList: KtTypeArgumentList, typeArgument: KtTypeProjection): KtTypeProjection

    /**
     * Adds [argument] to [argumentList].
     */
    fun appendValueArgument(argumentList: KtValueArgumentList, argument: KtValueArgument): KtValueArgument

    /**
     * Adds [argument] to [argumentList] after [anchor].
     */
    fun insertValueArgumentAfter(argumentList: KtValueArgumentList, argument: KtValueArgument, anchor: KtValueArgument?): KtValueArgument

    /**
     * Adds [argument] to [argumentList] before [anchor].
     */
    fun insertValueArgumentBefore(argumentList: KtValueArgumentList, argument: KtValueArgument, anchor: KtValueArgument?): KtValueArgument

    /**
     * Removes [argument] from [argumentList].
     */
    fun deleteValueArgument(argumentList: KtValueArgumentList, argument: KtValueArgument)

    /**
     * Removes an argument at [index] from [argumentList].
     */
    fun deleteValueArgument(argumentList: KtValueArgumentList, index: Int)

    /**
     * Removes [parameter] from [parameterList].
     */
    fun deleteParameter(parameterList: KtParameterList, parameter: KtParameter)

    /**
     * Removes a parameter at [index] from [parameterList].
     */
    fun deleteParameter(parameterList: KtParameterList, index: Int)

    /**
     * Returns the existing value parameter list for [functionLiteral], or creates an empty one together with the arrow token.
     */
    fun getOrCreateFunctionLiteralParameterList(functionLiteral: KtFunctionLiteral): KtParameterList

    /**
     * Returns the existing value argument list for [callExpression], or creates an empty one.
     */
    fun getOrCreateCallValueArgumentList(callExpression: KtCallExpression): KtValueArgumentList

    /**
     * Adds [typeArgument] to [callExpression], creating the type argument list if needed.
     */
    fun appendTypeArgument(callExpression: KtCallExpression, typeArgument: KtTypeProjection)

    /**
     * Replaces [element] with [newElement] on the AST level.
     */
    fun astReplace(element: PsiElement, newElement: PsiElement)

    /**
     * Replaces [expression] with [newElement], adding parentheses or string-template braces when needed.
     */
    fun replaceExpression(
        expression: KtExpression,
        newElement: PsiElement,
        reformat: Boolean,
        rawReplaceHandler: (PsiElement) -> PsiElement,
    ): PsiElement

    /**
     * Updates the text of [expression] for language injection.
     */
    fun updateStringTemplateText(expression: KtStringTemplateExpression, text: String): PsiLanguageInjectionHost

    /**
     * Updates the text of [section] for language injection.
     */
    fun updateKDocSectionText(section: KDocSection, text: String): PsiLanguageInjectionHost

    @KtNonPublicApi
    companion object {
        /**
         * Returns the registered Kotlin PSI mutation service.
         */
        @JvmStatic
        fun getInstance(): KtPsiMutationService =
            ApplicationManager.getApplication().getService(KtPsiMutationService::class.java)
                ?: throw IllegalStateException("Cannot mutate Kotlin PSI because KtPsiMutationService is missing")
    }
}
