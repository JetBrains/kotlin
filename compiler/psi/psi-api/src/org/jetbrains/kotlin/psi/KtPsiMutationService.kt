/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
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
