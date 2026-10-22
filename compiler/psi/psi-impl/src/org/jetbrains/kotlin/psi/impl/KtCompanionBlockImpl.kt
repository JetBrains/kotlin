/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.impl

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtCompanionBlock
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

internal class KtCompanionBlockImpl : KtElementImplStub<KotlinPlaceHolderStub<KtCompanionBlockImpl>>, KtCompanionBlock {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtCompanionBlockImpl>) : super(stub, KtNodeTypes.COMPANION_BLOCK)

    override val body: KtClassBody
        get() = getRequiredStubOrPsiChild(KtNodeTypes.CLASS_BODY, KtClassBody::class.java)

    override fun getDeclarations(): List<KtDeclaration> = body.declarations

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitCompanionBlock(this, data)
    }
}
