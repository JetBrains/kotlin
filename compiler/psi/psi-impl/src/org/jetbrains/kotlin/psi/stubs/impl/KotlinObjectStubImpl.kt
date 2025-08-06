/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinObjectStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    override val fqName: FqName?,
    override val classId: ClassId?,
    private val _superNames: Array<StringRef>,
    override val isTopLevel: Boolean,
    override val isLocal: Boolean,
    override val isObjectLiteral: Boolean,
) : KotlinStubBaseImpl<KtObjectDeclaration>(parent, KtStubElementTypes.OBJECT_DECLARATION), KotlinObjectStub {
    override fun getName() = StringRef.toString(name)
    override val superNames: List<String>
        get() = _superNames.map { it.toString() }
}
