/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinPropertyStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val isVar: Boolean,
    private val isTopLevel: Boolean,
    private val hasDelegate: Boolean,
    private val hasDelegateExpression: Boolean,
    private val hasInitializer: Boolean,
    private val isExtension: Boolean,
    private val isStaticExtension: Boolean,
    private val hasReturnTypeRef: Boolean,
    private val fqName: FqName?,
    val constantInitializer: ConstantValue<*>?,
    val origin: KotlinStubOrigin?,
    override val hasBackingField: Boolean?,
) : KotlinStubBaseImpl<KtProperty>(parent, KtStubElementTypes.PROPERTY), KotlinPropertyStub {

    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }
        if (hasDelegateExpression && !hasDelegate) {
            throw IllegalArgumentException("Can't have delegate expression without delegate")
        }
    }

    override fun getFqName() = fqName
    override fun isVar() = isVar
    override fun isTopLevel() = isTopLevel
    override fun hasDelegate() = hasDelegate
    override fun hasDelegateExpression() = hasDelegateExpression
    override fun hasInitializer() = hasInitializer
    override fun isExtension() = isExtension
    override fun isStaticExtension(): Boolean = isStaticExtension
    override fun hasReturnTypeRef() = hasReturnTypeRef
    override fun getName() = StringRef.toString(name)
}
