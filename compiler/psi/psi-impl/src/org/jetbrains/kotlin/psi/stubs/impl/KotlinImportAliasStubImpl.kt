/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinImportAliasStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
) : KotlinStubBaseImpl<KtImportAlias>(parent, KtStubElementTypes.IMPORT_ALIAS), KotlinImportAliasStub {
    override fun getName(): String? = StringRef.toString(name)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinImportAliasStubImpl = KotlinImportAliasStubImpl(
        parent = newParent,
        name = name
    )
}
