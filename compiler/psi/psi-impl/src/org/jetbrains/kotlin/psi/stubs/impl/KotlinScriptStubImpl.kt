/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinScriptStubImpl(
    parent: StubElement<*>?,
    private val fqNameRef: StringRef,
) : KotlinStubBaseImpl<KtScript>(parent, KtStubElementTypes.SCRIPT), KotlinScriptStub {
    override fun getName(): String = fqName.shortName().asString()
    override val fqName: FqName get() = FqName(fqNameRef.string)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinScriptStubImpl = KotlinScriptStubImpl(
        parent = newParent,
        fqNameRef = fqNameRef,
    )
}
