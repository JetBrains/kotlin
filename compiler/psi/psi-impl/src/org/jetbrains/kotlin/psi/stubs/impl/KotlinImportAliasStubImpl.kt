/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinImportAliasStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?
) : KotlinStubBaseImpl<KtImportAlias>(parent, KtStubElementTypes.IMPORT_ALIAS), KotlinImportAliasStub {
    override fun getName(): String? = StringRef.toString(name)
}
