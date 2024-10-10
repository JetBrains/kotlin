/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinPropertyStubImpl(
    parent: StubElement<*>,
    private val name: StringRef?,
    private val isVar: Boolean,
    private val isTopLevel: Boolean,
    private val hasInitializer: Boolean,
    private val isExtension: Boolean,
    private val hasReturnTypeRef: Boolean,
    private val fqName: FqName?,
    val constantInitializer: ConstantValue<*>?,
    val origin: KotlinStubOrigin?,
) : KotlinStubBaseImpl<KtProperty>(parent, KtStubElementTypes.PROPERTY), KotlinPropertyStub {

    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }
    }

    override fun getFqName(): FqName? = fqName
    override fun isVar(): Boolean = isVar
    override fun isTopLevel(): Boolean = isTopLevel
    override fun hasInitializer(): Boolean = hasInitializer
    override fun isExtension(): Boolean = isExtension
    override fun hasReturnTypeRef(): Boolean = hasReturnTypeRef
    override fun getName(): String? = StringRef.toString(name)
}
