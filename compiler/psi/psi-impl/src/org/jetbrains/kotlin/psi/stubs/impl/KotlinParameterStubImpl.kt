/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.KotlinParameterStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinParameterStubImpl(
    parent: StubElement<out PsiElement>?,
    private val _fqName: StringRef?,
    private val name: StringRef?,
    private val isMutable: Boolean,
    private val hasValOrVar: Boolean,
    private val hasDefaultValue: Boolean,
    val functionTypeParameterName: String? = null
) : KotlinStubBaseImpl<KtParameter>(parent, KtStubElementTypes.VALUE_PARAMETER), KotlinParameterStub {

    override fun getName(): String? {
        return StringRef.toString(name)
    }

    override val fqName: FqName?
        get() = if (_fqName != null) FqName(_fqName.string) else null

    override fun isMutable() = isMutable
    override fun hasValOrVar() = hasValOrVar
    override fun hasDefaultValue() = hasDefaultValue
}
