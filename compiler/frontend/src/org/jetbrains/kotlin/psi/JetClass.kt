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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

import java.util.ArrayList
import java.util.Collections

public open class JetClass : JetTypeParameterListOwnerStub<KotlinClassStub>, JetClassOrObject {

    public constructor(node: ASTNode) : super(node) {
    }

    public constructor(stub: KotlinClassStub) : super(stub, JetStubElementTypes.CLASS) {
    }

    override fun getDeclarations(): List<JetDeclaration> {
        val body = getBody() ?: return emptyList<JetDeclaration>()

        return body.getDeclarations()
    }

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitClass(this, data)
    }

    public fun getPrimaryConstructor(): JetPrimaryConstructor? {
        return getStubOrPsiChild(JetStubElementTypes.PRIMARY_CONSTRUCTOR)
    }

    public fun getPrimaryConstructorParameterList(): JetParameterList? {
        val primaryConstructor = getPrimaryConstructor()
        return primaryConstructor?.getValueParameterList()
    }

    public fun getPrimaryConstructorParameters(): List<JetParameter> {
        val list = getPrimaryConstructorParameterList() ?: return emptyList<JetParameter>()
        return list.getParameters()
    }

    public fun createPrimaryConstructorIfAbsent(): JetPrimaryConstructor {
        val constructor = getPrimaryConstructor()
        if (constructor != null) return constructor
        var anchor: PsiElement? = getTypeParameterList()
        if (anchor == null) anchor = getNameIdentifier()
        if (anchor == null) anchor = getLastChild()
        return addAfter(JetPsiFactory(getProject()).createPrimaryConstructor(), anchor) as JetPrimaryConstructor
    }

    public fun createPrimaryConstructorParameterListIfAbsent(): JetParameterList {
        val constructor = createPrimaryConstructorIfAbsent()
        val parameterList = constructor.getValueParameterList()
        if (parameterList != null) return parameterList
        return constructor.add(JetPsiFactory(getProject()).createParameterList("()")) as JetParameterList
    }

    override fun getDelegationSpecifierList(): JetDelegationSpecifierList? {
        return getStubOrPsiChild(JetStubElementTypes.DELEGATION_SPECIFIER_LIST)
    }

    override fun getDelegationSpecifiers(): List<JetDelegationSpecifier> {
        val list = getDelegationSpecifierList()
        return if (list != null) list.getDelegationSpecifiers() else emptyList<JetDelegationSpecifier>()
    }

    public fun getPrimaryConstructorModifierList(): JetModifierList? {
        val primaryConstructor = getPrimaryConstructor()
        return primaryConstructor?.getModifierList()
    }

    override fun getAnonymousInitializers(): List<JetClassInitializer> {
        val body = getBody() ?: return emptyList<JetClassInitializer>()

        return body.getAnonymousInitializers()
    }

    public fun hasExplicitPrimaryConstructor(): Boolean {
        return getPrimaryConstructor() != null
    }

    override fun getNameAsDeclaration(): JetObjectDeclarationName? {
        return findChildByType<PsiElement>(JetNodeTypes.OBJECT_DECLARATION_NAME) as JetObjectDeclarationName
    }

    override fun getBody(): JetClassBody? {
        return getStubOrPsiChild(JetStubElementTypes.CLASS_BODY)
    }

    public fun getColon(): PsiElement? {
        return findChildByType(JetTokens.COLON)
    }

    public fun getProperties(): List<JetProperty> {
        val body = getBody() ?: return emptyList<JetProperty>()

        return body.getProperties()
    }

    public fun isInterface(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isInterface()
        }

        return findChildByType<PsiElement>(JetTokens.TRAIT_KEYWORD) != null || findChildByType<PsiElement>(JetTokens.INTERFACE_KEYWORD) != null
    }

    public fun isEnum(): Boolean {
        return hasModifier(JetTokens.ENUM_KEYWORD)
    }

    public fun isAnnotation(): Boolean {
        return hasModifier(JetTokens.ANNOTATION_KEYWORD)
    }

    public fun isInner(): Boolean {
        return hasModifier(JetTokens.INNER_KEYWORD)
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (super<JetTypeParameterListOwnerStub>.isEquivalentTo(another)) {
            return true
        }
        if (another is JetClass) {
            val fq1 = getQualifiedName()
            val fq2 = another.getQualifiedName()
            return fq1 != null && fq2 != null && fq1 == fq2
        }
        return false
    }

    private fun getQualifiedName(): String? {
        val stub = getStub()
        if (stub != null) {
            val fqName = stub.getFqName()
            return fqName?.asString()
        }

        val parts = ArrayList<String>()
        var current: JetClassOrObject? = this
        while (current != null) {
            parts.add(current.getName())
            current = PsiTreeUtil.getParentOfType<JetClassOrObject>(current, javaClass<JetClassOrObject>())
        }
        val file = getContainingFile()
        if (file !is JetFile) return null
        val fileQualifiedName = file.getPackageFqName().asString()
        if (!fileQualifiedName.isEmpty()) {
            parts.add(fileQualifiedName)
        }
        Collections.reverse(parts)
        return StringUtil.join(parts, ".")
    }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    override fun isTopLevel(): Boolean {
        return getContainingFile() == getParent()
    }

    override fun isLocal(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isLocal()
        }
        return JetPsiUtil.isLocal(this)
    }

    public fun getCompanionObjects(): List<JetObjectDeclaration> {
        val body = getBody() ?: return emptyList<JetObjectDeclaration>()
        return body.getAllCompanionObjects()
    }

    public fun hasPrimaryConstructor(): Boolean {
        return hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()
    }

    private fun hasSecondaryConstructors(): Boolean {
        return !getSecondaryConstructors().isEmpty()
    }

    public fun getSecondaryConstructors(): List<JetSecondaryConstructor> {
        val body = getBody()
        return if (body != null) body.getSecondaryConstructors() else emptyList<JetSecondaryConstructor>()
    }

    public fun getClassOrInterfaceKeyword(): PsiElement? {
        return findChildByType(TokenSet.create(JetTokens.CLASS_KEYWORD, JetTokens.INTERFACE_KEYWORD))
    }
}
