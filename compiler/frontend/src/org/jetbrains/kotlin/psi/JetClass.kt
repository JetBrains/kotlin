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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import java.util.*

public open class JetClass : JetClassOrObject {
    public constructor(node: ASTNode) : super(node)
    public constructor(stub: KotlinClassStub) : super(stub, JetStubElementTypes.CLASS)

    override fun getStub(): KotlinClassStub? = super.getStub() as? KotlinClassStub

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitClass(this, data)
    }

    public fun getColon(): PsiElement? = findChildByType(JetTokens.COLON)

    public fun getProperties(): List<JetProperty> = getBody()?.getProperties().orEmpty()

    public fun isInterface(): Boolean =
        getStub()?.isInterface()
        ?: (findChildByType<PsiElement>(JetTokens.TRAIT_KEYWORD) != null || findChildByType<PsiElement>(JetTokens.INTERFACE_KEYWORD) != null)

    public fun isEnum(): Boolean = hasModifier(JetTokens.ENUM_KEYWORD)
    public fun isSealed(): Boolean = hasModifier(JetTokens.SEALED_KEYWORD)
    public fun isInner(): Boolean = hasModifier(JetTokens.INNER_KEYWORD)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (super.isEquivalentTo(another)) {
            return true
        }
        if (another is JetClass) {
            val fq1 = getQualifiedName()
            val fq2 = another.getQualifiedName()
            return fq1 != null && fq2 != null && fq1 == fq2
        }
        return false
    }

    protected fun getQualifiedName(): String? {
        val stub = getStub()
        if (stub != null) {
            val fqName = stub.getFqName()
            return fqName?.asString()
        }

        val parts = ArrayList<String>()
        var current: JetClassOrObject? = this
        while (current != null) {
            parts.add(current.getName()!!)
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

    public fun getCompanionObjects(): List<JetObjectDeclaration> = getBody()?.getAllCompanionObjects().orEmpty()

    public fun getClassOrInterfaceKeyword(): PsiElement? = findChildByType(TokenSet.create(JetTokens.CLASS_KEYWORD, JetTokens.INTERFACE_KEYWORD))
}
