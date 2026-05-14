/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.psi.KtNonPublicApi::class)
@file:Suppress("unused")

package org.jetbrains.kotlin.idea.base.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtCommonFile
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

/*
 * This is a temporary file with a set of alternative APIs for the now deprecated Kotlin PSI mutation methods.
 * These APIs are all marked "internal" and are not intended to be used from inside the kotlin repo itself
 * because they will shortly be moved to the Intellij Kotlin plugin source code (where they will become public).
 */

/**
 * Adds [superTypeListEntry] to this declaration's super type list.
 */
internal fun KtClassOrObject.addSuperType(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry {
    return KtPsiMutationService.getInstance().addSuperType(this, superTypeListEntry)
}

/**
 * Adds [superTypeListEntry] to this super type list.
 */
internal fun KtSuperTypeList.addSuperType(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry {
    return KtPsiMutationService.getInstance().addSuperType(this, superTypeListEntry)
}

/**
 * Removes [superTypeListEntry] from this declaration's super type list.
 */
internal fun KtClassOrObject.removeSuperType(superTypeListEntry: KtSuperTypeListEntry) {
    KtPsiMutationService.getInstance().removeSuperType(this, superTypeListEntry)
}

/**
 * Removes [superTypeListEntry] from this super type list.
 */
internal fun KtSuperTypeList.removeSuperType(superTypeListEntry: KtSuperTypeListEntry) {
    KtPsiMutationService.getInstance().removeSuperType(this, superTypeListEntry)
}

/**
 * Adds [declaration] to this declaration's body, creating a body when needed.
 */
internal fun <T : KtDeclaration> KtClassOrObject.addMemberDeclaration(declaration: T): T {
    return KtPsiMutationService.getInstance().addMemberDeclaration(this, declaration)
}

/**
 * Adds [declaration] after [anchor] in this declaration's body, or appends it when [anchor] is `null`.
 */
internal fun <T : KtDeclaration> KtClassOrObject.addMemberDeclarationAfter(declaration: T, anchor: PsiElement?): T {
    return KtPsiMutationService.getInstance().addMemberDeclarationAfter(this, declaration, anchor)
}

/**
 * Adds [declaration] before [anchor] in this declaration's body, or prepends it when [anchor] is `null`.
 */
internal fun <T : KtDeclaration> KtClassOrObject.addMemberDeclarationBefore(declaration: T, anchor: PsiElement?): T {
    return KtPsiMutationService.getInstance().addMemberDeclarationBefore(this, declaration, anchor)
}

/**
 * Returns the existing body for this declaration, or creates one if missing.
 */
internal fun KtClassOrObject.getOrCreateClassBody(): KtClassBody {
    return KtPsiMutationService.getInstance().getOrCreateClassBody(this)
}

/**
 * Adds a semicolon to this enum entry, reusing an existing sibling semicolon when possible.
 */
internal fun KtEnumEntry.addEnumEntrySemicolon(): PsiElement {
    return KtPsiMutationService.getInstance().addEnumEntrySemicolon(this)
}

/**
 * Returns the existing primary constructor for this class, or creates one if missing.
 */
internal fun KtClass.getOrCreatePrimaryConstructor(): KtPrimaryConstructor {
    return KtPsiMutationService.getInstance().getOrCreatePrimaryConstructor(this)
}

/**
 * Returns the existing primary constructor parameter list for this class, or creates one if missing.
 */
internal fun KtClass.getOrCreatePrimaryConstructorParameterList(): KtParameterList {
    return KtPsiMutationService.getInstance().getOrCreatePrimaryConstructorParameterList(this)
}

/**
 * Replaces this file's package name, adding a package directive when needed.
 */
@Suppress("DEPRECATION")
internal fun KtCommonFile.setPackageFqName(fqName: FqName) {
    KtPsiMutationService.getInstance().setPackageFqName(this, fqName)
}

/**
 * Replaces this package directive's package name.
 */
internal fun KtPackageDirective.setPackageFqName(fqName: FqName) {
    KtPsiMutationService.getInstance().setPackageFqName(this, fqName)
}

/**
 * Replaces this file's file annotation list with [annotationList], or adds it when missing.
 */
internal fun KtFile.replaceFileAnnotationList(annotationList: KtFileAnnotationList): KtFileAnnotationList {
    return KtPsiMutationService.getInstance().replaceFileAnnotationList(this, annotationList)
}

/**
 * Replaces this declaration's modifier list with [newModifierList], or adds it when missing.
 */
internal fun KtModifierListOwner.setModifierList(newModifierList: KtModifierList) {
    KtPsiMutationService.getInstance().setModifierList(this, newModifierList)
}

/**
 * Replaces this declaration's modifier list with [modifierList], adds it when missing, or removes it when [modifierList] is `null`.
 */
internal fun KtModifierListOwner.replaceModifierList(modifierList: KtModifierList?): KtModifierList? {
    return KtPsiMutationService.getInstance().replaceModifierList(this, modifierList)
}

/**
 * Adds [modifier] to this declaration's modifier list.
 */
internal fun KtModifierListOwner.addModifierKeyword(modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().addModifierKeyword(this, modifier)
}

/**
 * Adds [modifier] to this primary constructor's modifier list.
 */
internal fun KtPrimaryConstructor.addModifierKeyword(modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().addModifierKeyword(this, modifier)
}

/**
 * Removes [modifier] from this declaration's modifier list.
 */
internal fun KtModifierListOwner.removeModifierKeyword(modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().removeModifierKeyword(this, modifier)
}

/**
 * Removes [modifier] from this primary constructor's modifier list.
 */
internal fun KtPrimaryConstructor.removeModifierKeyword(modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().removeModifierKeyword(this, modifier)
}

/**
 * Adds [annotationEntry] to this declaration's modifier list.
 */
internal fun KtModifierListOwner.addAnnotation(annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
    return KtPsiMutationService.getInstance().addAnnotation(this, annotationEntry)
}

/**
 * Adds [annotationEntry] to this primary constructor's modifier list.
 */
internal fun KtPrimaryConstructor.addAnnotation(annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
    return KtPsiMutationService.getInstance().addAnnotation(this, annotationEntry)
}

/**
 * Removes [entry] from this annotation.
 */
internal fun KtAnnotation.removeAnnotationEntry(entry: KtAnnotationEntry) {
    KtPsiMutationService.getInstance().removeAnnotationEntry(this, entry)
}

/**
 * Removes this primary constructor's redundant `constructor` keyword.
 */
internal fun KtPrimaryConstructor.removeRedundantConstructorKeyword() {
    KtPsiMutationService.getInstance().removeRedundantConstructorKeyword(this)
}

/**
 * Replaces this function's return type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtNamedFunction.setFunctionTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setFunctionTypeReference(this, typeRef)
}

/**
 * Replaces this property's type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtProperty.setPropertyTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setPropertyTypeReference(this, typeRef)
}

/**
 * Replaces this property's initializer, adds it if missing, or removes it when [initializer] is `null`.
 */
internal fun KtProperty.setPropertyInitializer(initializer: KtExpression?): KtExpression? {
    return KtPsiMutationService.getInstance().setPropertyInitializer(this, initializer)
}

/**
 * Replaces this parameter's type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtParameter.setParameterTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setParameterTypeReference(this, typeRef)
}

/**
 * Replaces this destructuring entry's type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtDestructuringDeclarationEntry.setDestructuringDeclarationEntryTypeReference(
    typeRef: KtTypeReference?,
): KtTypeReference? {
    return KtPsiMutationService.getInstance().setDestructuringDeclarationEntryTypeReference(this, typeRef)
}

/**
 * Replaces this callable's explicit return type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtCallableDeclaration.setCallableTypeReference(
    addAfter: PsiElement?,
    typeRef: KtTypeReference?,
): KtTypeReference? {
    return KtPsiMutationService.getInstance().setCallableTypeReference(this, addAfter, typeRef)
}

/**
 * Replaces this callable's receiver type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtCallableDeclaration.setCallableReceiverTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setCallableReceiverTypeReference(this, typeRef)
}

/**
 * Replaces this function type's receiver type reference, adds it if missing, or removes it when [typeRef] is `null`.
 */
internal fun KtFunctionType.setFunctionTypeReceiverTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setFunctionTypeReceiverTypeReference(this, typeRef)
}

/**
 * Replaces this type parameter's extends bound, adds it if missing, or removes it when [typeReference] is `null`.
 */
internal fun KtTypeParameter.setTypeParameterExtendsBound(typeReference: KtTypeReference?): KtTypeReference? {
    return KtPsiMutationService.getInstance().setTypeParameterExtendsBound(this, typeReference)
}

/**
 * Replaces this double-colon expression's receiver expression, or adds it if missing.
 */
internal fun KtDoubleColonExpression.setDoubleColonReceiverExpression(newReceiverExpression: KtExpression) {
    KtPsiMutationService.getInstance().setDoubleColonReceiverExpression(this, newReceiverExpression)
}

/**
 * Removes this user type's qualifier, keeping the referenced name intact.
 */
internal fun KtUserType.removeQualifier() {
    KtPsiMutationService.getInstance().removeQualifier(this)
}

/**
 * Replaces this constructor's implicit delegation call with an explicit `this()` or `super()` call.
 */
internal fun KtSecondaryConstructor.convertImplicitDelegationCallToExplicit(isThis: Boolean): KtConstructorDelegationCall {
    return KtPsiMutationService.getInstance().convertImplicitDelegationCallToExplicit(this, isThis)
}

/**
 * Adds [parameter] to this parameter list.
 */
internal fun KtParameterList.appendParameter(parameter: KtParameter): KtParameter {
    return KtPsiMutationService.getInstance().appendParameter(this, parameter)
}

/**
 * Adds [parameter] before [anchor] in this parameter list.
 */
internal fun KtParameterList.insertParameterBefore(parameter: KtParameter, anchor: KtParameter?): KtParameter {
    return KtPsiMutationService.getInstance().insertParameterBefore(this, parameter, anchor)
}

/**
 * Adds [parameter] after [anchor] in this parameter list.
 */
internal fun KtParameterList.insertParameterAfter(parameter: KtParameter, anchor: KtParameter?): KtParameter {
    return KtPsiMutationService.getInstance().insertParameterAfter(this, parameter, anchor)
}

/**
 * Removes [parameter] from this parameter list.
 */
internal fun KtParameterList.deleteParameter(parameter: KtParameter) {
    KtPsiMutationService.getInstance().deleteParameter(this, parameter)
}

/**
 * Removes the parameter at [index] from this parameter list.
 */
internal fun KtParameterList.deleteParameter(index: Int) {
    KtPsiMutationService.getInstance().deleteParameter(this, index)
}

/**
 * Adds [typeParameter] to this type parameter list.
 */
internal fun KtTypeParameterList.appendTypeParameter(typeParameter: KtTypeParameter): KtTypeParameter {
    return KtPsiMutationService.getInstance().appendTypeParameter(this, typeParameter)
}

/**
 * Adds [typeArgument] to this type argument list.
 */
internal fun KtTypeArgumentList.appendTypeArgument(typeArgument: KtTypeProjection): KtTypeProjection {
    return KtPsiMutationService.getInstance().appendTypeArgument(this, typeArgument)
}

/**
 * Adds [argument] to this value argument list.
 */
internal fun KtValueArgumentList.appendValueArgument(argument: KtValueArgument): KtValueArgument {
    return KtPsiMutationService.getInstance().appendValueArgument(this, argument)
}

/**
 * Adds [argument] after [anchor] in this value argument list.
 */
internal fun KtValueArgumentList.insertValueArgumentAfter(argument: KtValueArgument, anchor: KtValueArgument?): KtValueArgument {
    return KtPsiMutationService.getInstance().insertValueArgumentAfter(this, argument, anchor)
}

/**
 * Adds [argument] before [anchor] in this value argument list.
 */
internal fun KtValueArgumentList.insertValueArgumentBefore(argument: KtValueArgument, anchor: KtValueArgument?): KtValueArgument {
    return KtPsiMutationService.getInstance().insertValueArgumentBefore(this, argument, anchor)
}

/**
 * Removes [argument] from this value argument list.
 */
internal fun KtValueArgumentList.deleteValueArgument(argument: KtValueArgument) {
    KtPsiMutationService.getInstance().deleteValueArgument(this, argument)
}

/**
 * Removes the value argument at [index] from this value argument list.
 */
internal fun KtValueArgumentList.deleteValueArgument(index: Int) {
    KtPsiMutationService.getInstance().deleteValueArgument(this, index)
}

/**
 * Returns this function literal's existing value parameter list, or creates one together with the arrow token.
 */
internal fun KtFunctionLiteral.getOrCreateFunctionLiteralParameterList(): KtParameterList {
    return KtPsiMutationService.getInstance().getOrCreateFunctionLiteralParameterList(this)
}

/**
 * Returns this call expression's existing value argument list, or creates one.
 */
internal fun KtCallExpression.getOrCreateCallValueArgumentList(): KtValueArgumentList {
    return KtPsiMutationService.getInstance().getOrCreateCallValueArgumentList(this)
}

/**
 * Adds [typeArgument] to this call expression, creating the type argument list when needed.
 */
internal fun KtCallExpression.appendTypeArgument(typeArgument: KtTypeProjection) {
    KtPsiMutationService.getInstance().appendTypeArgument(this, typeArgument)
}

/**
 * Replaces this element with [newElement] on the AST level.
 */
internal fun PsiElement.astReplace(newElement: PsiElement) {
    KtPsiMutationService.getInstance().astReplace(this, newElement)
}

/**
 * Replaces this expression with [newElement], adding parentheses or string-template braces when needed.
 */
internal fun KtExpression.replaceExpression(
    newElement: PsiElement,
    reformat: Boolean = true,
    rawReplaceHandler: (PsiElement) -> PsiElement,
): PsiElement {
    return KtPsiMutationService.getInstance().replaceExpression(this, newElement, reformat, rawReplaceHandler)
}
