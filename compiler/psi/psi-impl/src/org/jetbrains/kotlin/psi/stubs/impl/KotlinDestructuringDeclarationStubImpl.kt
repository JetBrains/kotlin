/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinDestructuringDeclarationStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
internal class KotlinDestructuringDeclarationStubImpl(
    parent: StubElement<*>?,
    override val isVar: Boolean,
    override val hasInitializer: Boolean,
) : KotlinStubBaseImpl<KtDestructuringDeclaration>(parent, KtStubElementTypes.DESTRUCTURING_DECLARATION),
    KotlinDestructuringDeclarationStub {

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinDestructuringDeclarationStubImpl =
        KotlinDestructuringDeclarationStubImpl(
            parent = newParent,
            isVar = isVar,
            hasInitializer = hasInitializer,
        )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinDestructuringDeclarationStubImpl &&
                other.isVar == isVar &&
                other.hasInitializer == hasInitializer
}
