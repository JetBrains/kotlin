/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtPlaceHolderStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

abstract class KtConstructor<T : KtConstructor<T>> : KtDeclarationStub<KotlinPlaceHolderStub<T>>, KtFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(stub: KotlinPlaceHolderStub<T>, nodeType: KtPlaceHolderStubElementType<T>) : super(stub, nodeType)

    abstract fun getContainingClassOrObject(): KtClassOrObject

    override fun isLocal() = false

    override fun getValueParameterList() = getStubOrPsiChild(KtStubElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters() = valueParameterList?.parameters ?: emptyList()

    override fun getReceiverTypeReference() = null

    override fun getContextReceiverTypeReferences(): List<KtTypeReference> = emptyList()

    override fun getTypeReference() = null

    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: KtTypeReference?) = throw IncorrectOperationException("setTypeReference to constructor")

    override fun getColon() = findChildByType<PsiElement>(KtTokens.COLON)

    override fun getBodyExpression(): KtBlockExpression? = null

    override fun getEqualsToken() = null

    override fun hasBlockBody() = bodyExpression != null

    override fun hasBody() = bodyExpression != null

    override fun hasDeclaredReturnType() = false

    override fun getTypeParameterList() = null

    override fun getTypeConstraintList() = null

    override fun getTypeConstraints() = emptyList<KtTypeConstraint>()

    override fun getTypeParameters() = emptyList<KtTypeParameter>()

    override fun getName(): String? = getContainingClassOrObject().name

    override fun getNameAsSafeName() = KtPsiUtil.safeName(name)

    override fun getFqName() = null

    override fun getNameAsName() = nameAsSafeName

    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = getConstructorKeyword()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    open fun getConstructorKeyword(): PsiElement? = findChildByType(KtTokens.CONSTRUCTOR_KEYWORD)

    fun hasConstructorKeyword(): Boolean = stub != null || getConstructorKeyword() != null

    override fun getTextOffset(): Int {
        return getConstructorKeyword()?.textOffset
                ?: valueParameterList?.textOffset
                ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingClassOrObject().useScope
    }
}
