/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinScriptStubImpl(
    parent: StubElement<out PsiElement>?,
    private val _fqName: StringRef?
) : KotlinStubBaseImpl<KtScript>(parent, KtStubElementTypes.SCRIPT), KotlinScriptStub {
    override fun getName(): String = fqName.shortName().asString()
    override val fqName: FqName get() = FqName(StringRef.toString(_fqName)!!)
}
