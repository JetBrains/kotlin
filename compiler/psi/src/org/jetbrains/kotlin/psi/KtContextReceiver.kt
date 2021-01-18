/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtContextReceiver : KtElementImplStub<KotlinPlaceHolderStub<KtContextReceiver>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContextReceiver>) : super(stub, KtStubElementTypes.CONTEXT_RECEIVER)

    fun targetLabel(): KtSimpleNameExpression? =
        findChildByType<KtContainerNode?>(KtNodeTypes.LABEL_QUALIFIER)
            ?.findChildByType(KtNodeTypes.LABEL)

    fun labelName(): String? = targetLabel()?.getReferencedName()
    fun labelNameAsName(): Name? = targetLabel()?.getReferencedNameAsName()

    fun typeReference(): KtTypeReference? = findChildByType(KtNodeTypes.TYPE_REFERENCE)

    fun name(): String? = labelName() ?: typeReference()?.nameForReceiverLabel()
}