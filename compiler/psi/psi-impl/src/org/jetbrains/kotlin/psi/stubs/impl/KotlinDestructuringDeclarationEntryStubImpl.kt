/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinDestructuringDeclarationEntryStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

internal class KotlinDestructuringDeclarationEntryStubImpl(
    parent: StubElement<*>?,
    private val name: StringRef?,
    override val isVar: Boolean,
    override val isSingleUnderscore: Boolean,
) : KotlinStubBaseImpl<KtDestructuringDeclarationEntry>(parent, KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY),
    KotlinDestructuringDeclarationEntryStub {

    override fun getName(): String? = StringRef.toString(name)

    // destructuring declaration entries don't have FqNames
    override val fqName: FqName? get() = null

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinDestructuringDeclarationEntryStubImpl =
        KotlinDestructuringDeclarationEntryStubImpl(
            parent = newParent,
            name = name,
            isVar = isVar,
            isSingleUnderscore = isSingleUnderscore,
        )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinDestructuringDeclarationEntryStubImpl &&
                other.name == name &&
                other.isVar == isVar &&
                other.isSingleUnderscore == isSingleUnderscore
}
