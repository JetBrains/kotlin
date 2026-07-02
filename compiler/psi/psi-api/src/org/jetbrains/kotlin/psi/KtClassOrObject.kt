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

/**
 * Represents a class, interface, object, or enum entry declaration.
 *
 * This is the common base for the concrete node types [KtClass] (classes and interfaces), [KtObjectDeclaration]
 * (named and companion objects), and [KtEnumEntry]. It gives access to the shared structure of such declarations:
 * the supertype list, the class body, the primary and secondary constructors, and the nested declarations.
 *
 * ### Example:
 *
 * ```kotlin
 *    class Foo : Bar() {
 *        fun baz() {}
 *    }
 * // ^__________________^
 * // The entire class-or-object declaration
 * ```
 */
abstract class KtClassOrObject :
    KtTypeParameterListOwnerStub<KotlinClassOrObjectStub<out KtClassOrObject>>, KtDeclarationContainer, KtNamedDeclaration,
    KtPureClassOrObject, KtClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassOrObjectStub<out KtClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    /**
     * Returns the colon token separating the declaration from its supertype list, or `null` if there is no supertype
     * list.
     */
    fun getColon(): PsiElement? = findChildByType(KtTokens.COLON)

    /**
     * Returns the supertype list (the types after the `:`), or `null` if this declaration has no explicit supertypes.
     */
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

    /**
     * Returns the `init` blocks declared in this class or object body, in source order; empty if there are none.
     */
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

    /**
     * Returns `true` if this declaration is a top-level member of a file (not nested in another declaration or a
     * local scope).
     */
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

    /**
     * Returns `true` if this declaration has the `data` modifier.
     */
    fun isData(): Boolean = hasModifier(KtTokens.DATA_KEYWORD)

    override fun getDeclarations(): List<KtDeclaration> = getBody()?.declarations.orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    override fun getPrimaryConstructor(): KtPrimaryConstructor? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR)

    override fun getPrimaryConstructorModifierList(): KtModifierList? = primaryConstructor?.modifierList

    /**
     * Returns the value parameter list of the primary constructor, or `null` if there is no explicit primary
     * constructor.
     */
    fun getPrimaryConstructorParameterList(): KtParameterList? = primaryConstructor?.valueParameterList

    override fun getPrimaryConstructorParameters(): List<KtParameter> = getPrimaryConstructorParameterList()?.parameters.orEmpty()

    override fun hasExplicitPrimaryConstructor(): Boolean = primaryConstructor != null

    override fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()

    /**
     * Returns `true` if this declaration has at least one secondary constructor.
     */
    fun hasSecondaryConstructors(): Boolean = !secondaryConstructors.isEmpty()

    override fun getSecondaryConstructors(): List<KtSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()

    /**
     * Returns `true` if this declaration has the `annotation` modifier (that is, it declares an annotation class).
     */
    fun isAnnotation(): Boolean = hasModifier(KtTokens.ANNOTATION_KEYWORD)

    /**
     * Returns the keyword that introduces this declaration (`class`, `interface`, or `object`), or `null` if it is
     * absent in incomplete code.
     */
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

/**
 * All constructors of this class or object: the primary constructor (if present) followed by the secondary
 * constructors, in source order.
 */
val KtClassOrObject.allConstructors
    get() = listOfNotNull(primaryConstructor) + secondaryConstructors
