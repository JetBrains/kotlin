/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.ClassIdCalculator
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

        val psiFactory = KtPsiFactory(this)
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
        return body.addAfter(declaration, anchor) as T
    }

    inline fun <reified T : KtDeclaration> addDeclarationAfter(declaration: T, anchor: PsiElement?): T {
        val anchorBefore = anchor ?: declarations.lastOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addAfter(declaration, anchorBefore) as T
    }

    inline fun <reified T : KtDeclaration> addDeclarationBefore(declaration: T, anchor: PsiElement?): T {
        val anchorAfter = anchor ?: declarations.firstOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addBefore(declaration, anchorAfter) as T
    }

    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is KtFile)

    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    override fun isLocal(): Boolean = stub?.isLocal() ?: KtPsiUtil.isLocal(this)

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

    fun getDeclarationKeyword(): PsiElement? =
        findChildByType(
            TokenSet.create(
                KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.OBJECT_KEYWORD
            )
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

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (this === another) {
            return true
        }

        if (another !is KtClassOrObject) {
            return false
        }

        val fq1 = getQualifiedName() ?: return false
        val fq2 = another.getQualifiedName() ?: return false
        if (fq1 == fq2) {
            val thisLocal = isLocal
            if (thisLocal != another.isLocal) {
                return false
            }

            // For non-local classes same fqn is enough
            // Consider different instances of local classes non-equivalent
            return !thisLocal
        }

        return false
    }

    protected fun getQualifiedName(): String? {
        val stub = stub
        if (stub != null) {
            val fqName = stub.getFqName()
            return fqName?.asString()
        }

        val parts = mutableListOf<String>()
        var current: KtClassOrObject? = this
        while (current != null) {
            val name = current.name ?: return null
            parts.add(name)
            current = PsiTreeUtil.getParentOfType(current, KtClassOrObject::class.java)
        }
        val file = containingFile as? KtFile ?: return null
        val fileQualifiedName = file.packageFqName.asString()
        if (!fileQualifiedName.isEmpty()) {
            parts.add(fileQualifiedName)
        }
        parts.reverse()
        return parts.joinToString(separator = ".")
    }

    override fun getContextReceiverTypeReferences(): List<KtTypeReference> {
        val stub = stub
        if (stub != null) {
            return getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE)
        }
        var node = node.firstChildNode
        while (node != null) {
            val tt = node.elementType
            if (tt === KtNodeTypes.CONTEXT_RECEIVER) {
                val contextReceiver = node.psi as KtContextReceiver
                return contextReceiver.typeReferences()
            }
            node = node.treeNext
        }
        return emptyList()
    }
}


fun KtClassOrObject.getOrCreateBody(): KtClassBody {
    getBody()?.let { return it }

    val newBody = KtPsiFactory(this).createEmptyClassBody()
    if (this is KtEnumEntry) return addAfter(newBody, initializerList ?: nameIdentifier) as KtClassBody
    return add(newBody) as KtClassBody
}

val KtClassOrObject.allConstructors
    get() = listOfNotNull(primaryConstructor) + secondaryConstructors
