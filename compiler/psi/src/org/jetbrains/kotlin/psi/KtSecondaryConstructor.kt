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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtSecondaryConstructor : KtConstructor<KtSecondaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinConstructorStub<KtSecondaryConstructor>) : super(stub, KtStubElementTypes.SECONDARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitSecondaryConstructor(this, data)

    override fun getContainingClassOrObject() = parent.parent as KtClassOrObject

    override fun getBodyExpression(): KtBlockExpression? {
        val stub = stub
        if (stub != null) {
            if (stub.hasBody() == false) {
                return null
            }
            if (containingKtFile.isCompiled) {
                return null
            }
        }
        return findChildByClass(KtBlockExpression::class.java)
    }

    override fun getConstructorKeyword() = notNullChild<PsiElement>(super.getConstructorKeyword())

    fun getDelegationCall(): KtConstructorDelegationCall = findNotNullChildByClass(KtConstructorDelegationCall::class.java)

    fun getDelegationCallOrNull(): KtConstructorDelegationCall? = findChildByClass(KtConstructorDelegationCall::class.java)

    fun hasImplicitDelegationCall(): Boolean = getDelegationCall().isImplicit

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): KtConstructorDelegationCall {
        val psiFactory = KtPsiFactory(project)
        val current = getDelegationCall()

        assert(current.isImplicit) { "Method should not be called with explicit delegation call: " + text }
        current.delete()

        val colon = addAfter(psiFactory.createColon(), valueParameterList)

        val delegationName = if (isThis) "this" else "super"

        return addAfter(psiFactory.creareDelegatedSuperTypeEntry(delegationName + "()"), colon) as KtConstructorDelegationCall
    }
}
