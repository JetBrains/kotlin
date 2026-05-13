/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtNonPublicApi::class)

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.psiUtil.ClassIdCalculator
import org.jetbrains.kotlin.psi.psiUtil.isKtFile
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub

abstract class KtClassOrObject :
    KtTypeParameterListOwnerStub<KotlinClassOrObjectStub<out KtClassOrObject>>, KtDeclarationContainer, KtNamedDeclaration,
    KtPureClassOrObject, KtClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassOrObjectStub<out KtClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    fun getColon(): PsiElement? = findChildByType(KtTokens.COLON)

    fun getSuperTypeList(): KtSuperTypeList? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.SUPER_TYPE_LIST)

    override fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    @Deprecated(
        "Use addSuperType(superTypeListEntry) instead",
        ReplaceWith("this.addSuperType(superTypeListEntry)", "org.jetbrains.kotlin.idea.base.psi.addSuperType"),
    )
    fun addSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry =
        KtPsiMutationService.getInstance().addSuperType(this, superTypeListEntry)

    @Deprecated(
        "Use removeSuperType(superTypeListEntry) instead",
        ReplaceWith("this.removeSuperType(superTypeListEntry)", "org.jetbrains.kotlin.idea.base.psi.removeSuperType"),
    )
    fun removeSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry) {
        KtPsiMutationService.getInstance().removeSuperType(this, superTypeListEntry)
    }

    fun getAnonymousInitializers(): List<KtAnonymousInitializer> = getBody()?.anonymousInitializers.orEmpty()

    override fun getBody(): KtClassBody? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.CLASS_BODY)

    @Deprecated(
        "Use addMemberDeclaration(declaration) instead",
        ReplaceWith("this.addMemberDeclaration(declaration)", "org.jetbrains.kotlin.idea.base.psi.addMemberDeclaration"),
    )
    inline fun <reified T : KtDeclaration> addDeclaration(declaration: T): T =
        KtPsiMutationService.getInstance().addMemberDeclaration(this, declaration)

    @Deprecated(
        "Use addMemberDeclarationAfter(declaration, anchor) instead",
        ReplaceWith(
            "this.addMemberDeclarationAfter(declaration, anchor)",
            "org.jetbrains.kotlin.idea.base.psi.addMemberDeclarationAfter",
        ),
    )
    inline fun <reified T : KtDeclaration> addDeclarationAfter(declaration: T, anchor: PsiElement?): T =
        KtPsiMutationService.getInstance().addMemberDeclarationAfter(this, declaration, anchor)

    @Deprecated(
        "Use addMemberDeclarationBefore(declaration, anchor) instead",
        ReplaceWith(
            "this.addMemberDeclarationBefore(declaration, anchor)",
            "org.jetbrains.kotlin.idea.base.psi.addMemberDeclarationBefore",
        ),
    )
    inline fun <reified T : KtDeclaration> addDeclarationBefore(declaration: T, anchor: PsiElement?): T =
        KtPsiMutationService.getInstance().addMemberDeclarationBefore(this, declaration, anchor)

    fun isTopLevel(): Boolean = greenStub?.isTopLevel ?: isKtFile(parent)

    override fun getClassId(): ClassId? {
        greenStub?.let { return it.classId }

        if (isLocal()) return null

        return ClassIdCalculator.calculateClassId(this)
    }

    @Volatile
    private var isLocal: Boolean? = null

    override fun isLocal(): Boolean {
        greenStub?.isLocal?.let { return it }

        isLocal?.let { return it }

        return KtPsiUtil.isLocal(this).also {
            isLocal = it
        }
    }

    fun isData(): Boolean = hasModifier(KtTokens.DATA_KEYWORD)

    override fun getDeclarations(): List<KtDeclaration> = getBody()?.declarations.orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    override fun getPrimaryConstructor(): KtPrimaryConstructor? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR)

    override fun getPrimaryConstructorModifierList(): KtModifierList? = primaryConstructor?.modifierList

    fun getPrimaryConstructorParameterList(): KtParameterList? = primaryConstructor?.valueParameterList

    override fun getPrimaryConstructorParameters(): List<KtParameter> = getPrimaryConstructorParameterList()?.parameters.orEmpty()

    override fun hasExplicitPrimaryConstructor(): Boolean = primaryConstructor != null

    override fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()

    fun hasSecondaryConstructors(): Boolean = !secondaryConstructors.isEmpty()

    override fun getSecondaryConstructors(): List<KtSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()

    fun isAnnotation(): Boolean = hasModifier(KtTokens.ANNOTATION_KEYWORD)

    fun getDeclarationKeyword(): PsiElement? = findChildByType(classInterfaceObjectTokenSet)

    /**
     * The list of all companion blocks.
     */
    @KtExperimentalApi
    val companionBlocks: List<KtCompanionBlock>
        get() = body?.companionBlocks.orEmpty()

    private val classInterfaceObjectTokenSet = TokenSet.create(
        KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.OBJECT_KEYWORD
    )

    override fun delete() {
        KtPsiMutationService.getInstance().deleteClassOrObject(this)
    }

    override fun subtreeChanged() {
        // most likely, we may not drop isLocal as the class shouldn't survive such a destructive change
        isLocal = null
        super.subtreeChanged()
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        this === another ||
                another is KtClassOrObject &&
                // Consider different instances of local classes non-equivalent
                !isLocal() &&
                !another.isLocal() &&
                getClassId() == another.getClassId()

    override fun getContextReceivers(): List<KtContextReceiver> =
        modifierList?.contextParameterList?.contextReceivers().orEmpty()
}


@Deprecated(
    "Use getOrCreateClassBody() instead",
    ReplaceWith("this.getOrCreateClassBody()", "org.jetbrains.kotlin.idea.base.psi.getOrCreateClassBody"),
)
fun KtClassOrObject.getOrCreateBody(): KtClassBody = KtPsiMutationService.getInstance().getOrCreateClassBody(this)

val KtClassOrObject.allConstructors
    get() = listOfNotNull(primaryConstructor) + secondaryConstructors
