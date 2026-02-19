/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinTypeParameterStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
) : KotlinStubBaseImpl<KtTypeParameter>(parent, KtStubElementTypes.TYPE_PARAMETER), KotlinTypeParameterStub {
    override fun getName(): String? = StringRef.toString(name)

    // type parameters don't have FqNames
    override val fqName: FqName? get() = null

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinTypeParameterStubImpl = KotlinTypeParameterStubImpl(
        parent = newParent,
        name = name,
    )
}
