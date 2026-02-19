/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.KotlinParameterStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinParameterStubImpl(
    parent: StubElement<*>?,
    private val fqNameRef: StringRef?,
    private val name: StringRef?,
    override val isMutable: Boolean,
    override val hasValOrVar: Boolean,
    override val hasDefaultValue: Boolean,
    val functionTypeParameterName: String?,
) : KotlinStubBaseImpl<KtParameter>(parent, KtStubElementTypes.VALUE_PARAMETER), KotlinParameterStub {

    override fun getName(): String? = name?.string

    // val/var parameters from a primary constructor might have fqName
    override val fqName: FqName?
        get() = fqNameRef?.string?.let(::FqName)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinParameterStubImpl = KotlinParameterStubImpl(
        parent = newParent,
        fqNameRef = fqNameRef,
        name = name,
        isMutable = isMutable,
        hasValOrVar = hasValOrVar,
        hasDefaultValue = hasDefaultValue,
        functionTypeParameterName = functionTypeParameterName,
    )
}
