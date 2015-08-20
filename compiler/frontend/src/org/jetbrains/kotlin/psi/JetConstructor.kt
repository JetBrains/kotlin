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
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.JetPlaceHolderStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

public abstract class JetConstructor<T : JetConstructor<T>> : JetDeclarationStub<KotlinPlaceHolderStub<T>>, JetFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(stub: KotlinPlaceHolderStub<T>, nodeType: JetPlaceHolderStubElementType<T>) : super(stub, nodeType)

    public abstract fun getContainingClassOrObject(): JetClassOrObject

    override fun isLocal() = false

    override fun getValueParameterList() = getStubOrPsiChild(JetStubElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters() = getValueParameterList()?.getParameters() ?: emptyList()

    override fun getReceiverTypeReference() = null

    override fun getTypeReference() = null

    throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: JetTypeReference?) = throw IncorrectOperationException("setTypeReference to constructor")

    override fun getColon() = findChildByType<PsiElement>(JetTokens.COLON)

    override fun getBodyExpression(): JetBlockExpression? = null

    override fun getEqualsToken() = null

    override fun hasBlockBody() = true

    override fun hasBody() = getBodyExpression() != null

    override fun hasDeclaredReturnType() = false

    override fun getTypeParameterList() = null

    override fun getTypeConstraintList() = null

    override fun getTypeConstraints() = emptyList<JetTypeConstraint>()

    override fun getTypeParameters() = emptyList<JetTypeParameter>()

    override fun getName(): String? = getContainingClassOrObject().getName()

    override fun getNameAsSafeName() = JetPsiUtil.safeName(getName())

    override fun getFqName() = null

    override fun getNameAsName() = getNameAsSafeName()

    override fun getNameIdentifier() = null

    throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    public open fun getConstructorKeyword(): PsiElement? = findChildByType(JetTokens.CONSTRUCTOR_KEYWORD)

    public fun hasConstructorKeyword(): Boolean = getStub() != null || getConstructorKeyword() != null

    override fun getTextOffset(): Int {
        return getConstructorKeyword()?.getTextOffset()
               ?: getValueParameterList()?.getTextOffset()
               ?: super<JetDeclarationStub>.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingClassOrObject().getUseScope()
    }
}
