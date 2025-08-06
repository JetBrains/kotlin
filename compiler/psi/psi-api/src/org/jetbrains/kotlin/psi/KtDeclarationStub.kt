/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import java.util.concurrent.atomic.AtomicLong

abstract class KtDeclarationStub<T : StubElement<*>> : KtModifierListOwnerStub<T>, KtDeclaration {
    private val _modificationStamp = AtomicLong()

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override fun subtreeChanged() {
        super.subtreeChanged()
        _modificationStamp.getAndIncrement()
    }

    val modificationStamp: Long get() = _modificationStamp.get()

    override fun getDocComment(): KDoc? {
        return findDocComment(this)
    }

    override fun getParent(): PsiElement {
        val stub = getStub()
        // we build stubs for local classes/objects too but they have wrong parent
        if ((stub as? KotlinClassOrObjectStub<*>)?.isLocal() == true) {
            return stub.parentStub.getPsi()
        }

        return super.getParent()
    }

    override fun getOriginalElement(): PsiElement {
        val navigationPolicy = ApplicationManager.getApplication().serviceOrNull<KotlinDeclarationNavigationPolicy>()
        return navigationPolicy?.getOriginalElement(this) ?: this
    }

    override fun getNavigationElement(): PsiElement {
        val navigationPolicy = ApplicationManager.getApplication().serviceOrNull<KotlinDeclarationNavigationPolicy>()
        return navigationPolicy?.getNavigationElement(this) ?: this
    }
}
