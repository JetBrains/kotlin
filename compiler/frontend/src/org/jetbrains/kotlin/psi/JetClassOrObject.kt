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
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.stubs.IStubElementType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

abstract public class JetClassOrObject : JetTypeParameterListOwnerStub<KotlinClassOrObjectStub<out JetClassOrObject>>, JetDeclarationContainer, JetNamedDeclaration {
    public constructor(node: ASTNode) : super(node)
    public constructor(stub: KotlinClassOrObjectStub<out JetClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    public fun getDelegationSpecifierList(): JetDelegationSpecifierList? = getStubOrPsiChild(JetStubElementTypes.DELEGATION_SPECIFIER_LIST)
    open public fun getDelegationSpecifiers(): List<JetDelegationSpecifier> = getDelegationSpecifierList()?.getDelegationSpecifiers().orEmpty()

    public fun getAnonymousInitializers(): List<JetClassInitializer> = getBody()?.getAnonymousInitializers().orEmpty()

    public fun getNameAsDeclaration(): JetObjectDeclarationName? =
            findChildByType<PsiElement>(JetNodeTypes.OBJECT_DECLARATION_NAME) as JetObjectDeclarationName?

    public fun getBody(): JetClassBody? = getStubOrPsiChild(JetStubElementTypes.CLASS_BODY)

    public fun isTopLevel(): Boolean = getStub()?.isTopLevel() ?: (getParent() is JetFile)

    public fun isLocal(): Boolean = getStub()?.isLocal() ?: JetPsiUtil.isLocal(this)
    
    override fun getDeclarations() = getBody()?.getDeclarations().orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    public fun getPrimaryConstructor(): JetPrimaryConstructor? = getStubOrPsiChild(JetStubElementTypes.PRIMARY_CONSTRUCTOR)

    public fun getPrimaryConstructorModifierList(): JetModifierList? = getPrimaryConstructor()?.getModifierList()
    public fun getPrimaryConstructorParameterList(): JetParameterList? = getPrimaryConstructor()?.getValueParameterList()
    public fun getPrimaryConstructorParameters(): List<JetParameter> = getPrimaryConstructorParameterList()?.getParameters().orEmpty()

    public fun hasExplicitPrimaryConstructor(): Boolean = getPrimaryConstructor() != null

    public fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()
    private fun hasSecondaryConstructors(): Boolean = !getSecondaryConstructors().isEmpty()

    public fun getSecondaryConstructors(): List<JetSecondaryConstructor> = getBody()?.getSecondaryConstructors().orEmpty()

    deprecated(value = "It's no more possible to determine it exactly using AST. Use ClassDescriptor methods instead, e.g. getKind()")
    public fun isAnnotation(): Boolean = hasAnnotation(KotlinBuiltIns.FQ_NAMES.annotation.shortName().asString())

    private fun hasAnnotation(name: String): Boolean {
        for (entry in getAnnotationEntries()) {
            val typeReference = entry.getTypeReference()
            val userType = typeReference?.getStubOrPsiChild(JetStubElementTypes.USER_TYPE) ?: continue
            if (name == userType.getReferencedName()) return true
        }
        return false
    }

    public override fun delete() {
        CheckUtil.checkWritable(this);

        val file = getContainingJetFile();
        if (!isTopLevel() || file.getDeclarations().size() > 1) {
            CodeEditUtil.removeChild(getParent().getNode(), getNode());
        }
        else {
            file.delete();
        }
    }
}
