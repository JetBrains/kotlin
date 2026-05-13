/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement

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
