/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.util.*

open class KtClass : KtClassOrObject {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassStub) : super(stub, KtStubElementTypes.CLASS)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitClass(this, data)
    }

    private val _stub: KotlinClassStub?
        get() = stub as? KotlinClassStub

    fun getColon(): PsiElement? = findChildByType(KtTokens.COLON)

    fun getProperties(): List<KtProperty> = getBody()?.properties.orEmpty()

    fun isInterface(): Boolean =
        _stub?.isInterface() ?: (findChildByType<PsiElement>(KtTokens.INTERFACE_KEYWORD) != null)

    fun isEnum(): Boolean = hasModifier(KtTokens.ENUM_KEYWORD)
    fun isData(): Boolean = hasModifier(KtTokens.DATA_KEYWORD)
    fun isSealed(): Boolean = hasModifier(KtTokens.SEALED_KEYWORD)
    fun isInner(): Boolean = hasModifier(KtTokens.INNER_KEYWORD)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (super.isEquivalentTo(another)) {
            return true
        }
        if (another is KtClass) {
            val fq1 = getQualifiedName()
            val fq2 = another.getQualifiedName()
            return fq1 != null && fq2 != null && fq1 == fq2
        }
        return false
    }

    protected fun getQualifiedName(): String? {
        val stub = stub
        if (stub != null) {
            val fqName = stub.getFqName()
            return fqName?.asString()
        }

        val parts = ArrayList<String>()
        var current: KtClassOrObject? = this
        while (current != null) {
            parts.add(current.name!!)
            current = PsiTreeUtil.getParentOfType<KtClassOrObject>(current, KtClassOrObject::class.java)
        }
        val file = containingFile as? KtFile ?: return null
        val fileQualifiedName = file.packageFqName.asString()
        if (!fileQualifiedName.isEmpty()) {
            parts.add(fileQualifiedName)
        }
        Collections.reverse(parts)
        return StringUtil.join(parts, ".")
    }

    override fun getCompanionObjects(): List<KtObjectDeclaration> = getBody()?.allCompanionObjects.orEmpty()

    fun getClassOrInterfaceKeyword(): PsiElement? = findChildByType(TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD))
}

fun KtClass.createPrimaryConstructorIfAbsent(): KtPrimaryConstructor {
    val constructor = primaryConstructor
    if (constructor != null) return constructor
    var anchor: PsiElement? = typeParameterList
    if (anchor == null) anchor = nameIdentifier
    if (anchor == null) anchor = lastChild
    return addAfter(KtPsiFactory(project).createPrimaryConstructor(), anchor) as KtPrimaryConstructor
}

fun KtClass.createPrimaryConstructorParameterListIfAbsent(): KtParameterList {
    val constructor = createPrimaryConstructorIfAbsent()
    val parameterList = constructor.valueParameterList
    if (parameterList != null) return parameterList
    return constructor.add(KtPsiFactory(project).createParameterList("()")) as KtParameterList
}