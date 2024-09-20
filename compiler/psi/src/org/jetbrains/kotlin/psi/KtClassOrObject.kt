/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.psiUtil.ClassIdCalculator
import org.jetbrains.kotlin.psi.psiUtil.isKtFile
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

abstract class KtClassOrObject :
    KtTypeParameterListOwnerStub<KotlinClassOrObjectStub<out KtClassOrObject>>, KtDeclarationContainer, KtNamedDeclaration,
    KtPureClassOrObject, KtClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassOrObjectStub<out KtClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    fun getColon(): PsiElement? = findChildByType(KtTokens.COLON)

    fun getSuperTypeList(): KtSuperTypeList? = getStubOrPsiChild(KtStubElementTypes.SUPER_TYPE_LIST)

    override fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    fun addSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry {
        getSuperTypeList()?.let {
            val single = it.entries.singleOrNull()
            if (single != null && single.typeReference?.typeElement == null) {
                return single.replace(superTypeListEntry) as KtSuperTypeListEntry
            }
            return EditCommaSeparatedListHelper.addItem(it, superTypeListEntries, superTypeListEntry)
        }

        val psiFactory = KtPsiFactory(project)
        val specifierListToAdd = psiFactory.createSuperTypeCallEntry("A()").replace(superTypeListEntry).parent
        val colon = addBefore(psiFactory.createColon(), getBody())
        return (addAfter(specifierListToAdd, colon) as KtSuperTypeList).entries.first()
    }

    fun removeSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry) {
        val specifierList = getSuperTypeList() ?: return
        assert(superTypeListEntry.parent === specifierList)

        if (specifierList.entries.size > 1) {
            EditCommaSeparatedListHelper.removeItem<KtElement>(superTypeListEntry)
        } else {
            deleteChildRange(getColon() ?: specifierList, specifierList)
        }
    }

    fun getAnonymousInitializers(): List<KtAnonymousInitializer> = getBody()?.anonymousInitializers.orEmpty()

    override fun getBody(): KtClassBody? = getStubOrPsiChild(KtStubElementTypes.CLASS_BODY)

    inline fun <reified T : KtDeclaration> addDeclaration(declaration: T): T {
        val body = getOrCreateBody()
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.lastChild!!, PsiWhiteSpace::class.java)
        return if (anchor?.nextSibling is PsiErrorElement) {
            body.addBefore(declaration, anchor)
        } else {
            body.addAfter(declaration, anchor)
        } as T
    }

    inline fun <reified T : KtDeclaration> addDeclarationAfter(declaration: T, anchor: PsiElement?): T {
        val anchorBefore = anchor ?: declarations.lastOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addAfter(declaration, anchorBefore) as T
    }

    inline fun <reified T : KtDeclaration> addDeclarationBefore(declaration: T, anchor: PsiElement?): T {
        val anchorAfter = anchor ?: declarations.firstOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addBefore(declaration, anchorAfter) as T
    }

    fun isTopLevel(): Boolean = greenStub?.isTopLevel() ?: isKtFile(parent)

    override fun getClassId(): ClassId? {
        greenStub?.let { return it.getClassId() }

        if (isLocal()) return null

        return ClassIdCalculator.calculateClassId(this)
    }

    @Volatile
    private var isLocal: Boolean? = null

    override fun isLocal(): Boolean {
        greenStub?.isLocal()?.let { return it }

        isLocal?.let { return it }

        return KtPsiUtil.isLocal(this).also {
            isLocal = it
        }
    }

    fun isData(): Boolean = hasModifier(KtTokens.DATA_KEYWORD)

    override fun getDeclarations(): List<KtDeclaration> = getBody()?.declarations.orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    override fun getPrimaryConstructor(): KtPrimaryConstructor? = getStubOrPsiChild(KtStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun getPrimaryConstructorModifierList(): KtModifierList? = primaryConstructor?.modifierList

    fun getPrimaryConstructorParameterList(): KtParameterList? = primaryConstructor?.valueParameterList

    override fun getPrimaryConstructorParameters(): List<KtParameter> = getPrimaryConstructorParameterList()?.parameters.orEmpty()

    override fun hasExplicitPrimaryConstructor(): Boolean = primaryConstructor != null

    override fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()

    fun hasSecondaryConstructors(): Boolean = !secondaryConstructors.isEmpty()

    override fun getSecondaryConstructors(): List<KtSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()

    fun isAnnotation(): Boolean = hasModifier(KtTokens.ANNOTATION_KEYWORD)

    fun getDeclarationKeyword(): PsiElement? = findChildByType(classInterfaceObjectTokenSet)

    private val classInterfaceObjectTokenSet = TokenSet.create(
        KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.OBJECT_KEYWORD
    )

    override fun delete() {
        CheckUtil.checkWritable(this)

        val file = containingKtFile
        if (!isTopLevel() || file.declarations.size > 1) {
            super.delete()
        } else {
            file.delete()
        }
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

    fun getContextReceiverList(): KtContextReceiverList? = getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST)

    override fun getContextReceivers(): List<KtContextReceiver> =
        getContextReceiverList()?.let { return it.contextReceivers() } ?: emptyList()
}


fun KtClassOrObject.getOrCreateBody(): KtClassBody {
    getBody()?.let { return it }

    val newBody = KtPsiFactory(project).createEmptyClassBody()
    if (this is KtEnumEntry) return addAfter(newBody, initializerList ?: nameIdentifier) as KtClassBody
    return add(newBody) as KtClassBody
}

val KtClassOrObject.allConstructors
    get() = listOfNotNull(primaryConstructor) + secondaryConstructors
