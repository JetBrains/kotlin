/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KotlinPsiModificationUtils")
@file:OptIn(org.jetbrains.kotlin.psi.KtNonPublicApi::class)

package org.jetbrains.kotlin.idea.base.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

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
