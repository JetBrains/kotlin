/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinImportDirectiveStubImpl(
    parent: StubElement<*>?,
    override val isAllUnder: Boolean,
    override val isPackage: Boolean,
    private val importedFqNameRef: StringRef?,
    override val isValid: Boolean,
) : KotlinStubBaseImpl<KtImportDirective>(parent, KtStubElementTypes.IMPORT_DIRECTIVE), KotlinImportDirectiveStub {
    override val importedFqName: FqName?
        get() = importedFqNameRef?.string?.let(::FqName)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = newParent,
        isAllUnder = isAllUnder,
        isPackage = isPackage,
        importedFqNameRef = importedFqNameRef,
        isValid = isValid,
    )
}
