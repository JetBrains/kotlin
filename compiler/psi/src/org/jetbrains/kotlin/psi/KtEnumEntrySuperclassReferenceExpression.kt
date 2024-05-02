/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinEnumEntrySuperclassReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.KtResolvableCall

// This node represents "fake" reference expression for ENUM_ENTRY(arguments) constructor syntax
// It uses the superclass enum node to provide access to the real constructor name
class KtEnumEntrySuperclassReferenceExpression :
    KtExpressionImplStub<KotlinEnumEntrySuperclassReferenceExpressionStub>, KtSimpleNameExpression, KtResolvableCall {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinEnumEntrySuperclassReferenceExpressionStub) :
            super(stub, KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION)

    // It is the owner enum class (not an enum entry but the whole enum)
    private val referencedElement: KtClass?
        get() = calcReferencedElement()

    private fun calcReferencedElement(): KtClass? {
        val owner = this.getStrictParentOfType<KtEnumEntry>()
        return owner?.parent?.parent as? KtClass
    }

    override fun getReferencedName(): String {
        val stub = stub
        if (stub != null) {
            return stub.getReferencedName()
        }
        val text = (getReferencedNameElement() as KtNamedDeclaration).nameAsSafeName.asString()
        return KtPsiUtil.unquoteIdentifierOrFieldReference(text)
    }

    override fun getReferencedNameAsName(): Name {
        return referencedElement?.name?.let { Name.identifier(it) } ?: SpecialNames.NO_NAME_PROVIDED
    }

    override fun getReferencedNameElement(): PsiElement {
        return referencedElement!!
    }

    override fun getIdentifier(): PsiElement? {
        return referencedElement?.nameIdentifier
    }

    override fun getReferencedNameElementType(): IElementType {
        return getReferencedNameElement().node!!.elementType
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }
}

