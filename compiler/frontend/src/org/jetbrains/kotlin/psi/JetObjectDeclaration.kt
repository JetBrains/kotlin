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
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

public class JetObjectDeclaration : JetClassOrObject {
    public constructor(node: ASTNode) : super(node)
    public constructor(stub: KotlinObjectStub) : super(stub, JetStubElementTypes.OBJECT_DECLARATION)

    override fun getStub(): KotlinObjectStub? = super.getStub() as? KotlinObjectStub

    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }

        val nameAsDeclaration = getNameAsDeclaration()
        if (nameAsDeclaration == null && isCompanion()) {
            //NOTE: a hack in PSI that simplifies writing frontend code
            return SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.toString()
        }
        return nameAsDeclaration?.name
    }

    override fun getNameIdentifier(): PsiElement? = getNameAsDeclaration()?.nameIdentifier

    override fun setName(@NonNls name: String): PsiElement {
        val declarationName = getNameAsDeclaration()
        if (declarationName == null) {
            val psiFactory = JetPsiFactory(project)
            val result = addAfter(psiFactory.createObjectDeclarationName(name), getObjectKeyword())
            addAfter(psiFactory.createWhiteSpace(), getObjectKeyword())

            return result
        }
        else {
            return declarationName.setName(name)
        }
    }

    public fun isCompanion(): Boolean = stub?.isCompanion() ?: hasModifier(JetTokens.COMPANION_KEYWORD)

    override fun getTextOffset(): Int = nameIdentifier?.textRange?.startOffset
                                        ?: getObjectKeyword().textRange.startOffset

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitObjectDeclaration(this, data)
    }

    public fun isObjectLiteral(): Boolean = stub?.isObjectLiteral() ?: (parent is JetObjectLiteralExpression)

    public fun getObjectKeyword(): PsiElement = findChildByType(JetTokens.OBJECT_KEYWORD)!!
}
