/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@OptIn(KtImplementationDetail::class)
class KotlinModifierListStubImpl(
    parent: StubElement<*>?,
    internal val mask: Long,
) : KotlinStubBaseImpl<KtDeclarationModifierList>(parent, KtNodeTypes.MODIFIER_LIST), KotlinModifierListStub {
    override fun hasModifier(modifierToken: KtModifierKeywordToken): Boolean = ModifierMaskUtils.maskHasModifier(mask, modifierToken)

    @KtImplementationDetail
    override fun hasSpecialFlag(flag: KotlinModifierListStub.SpecialFlag): Boolean = ModifierMaskUtils.maskHasSpecialFlag(mask, flag)

    fun hasAnyModifier(): Boolean = mask != 0L

    override fun toString(): String = super.toString() + ModifierMaskUtils.maskToString(mask)

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parent = newParent,
        mask = mask,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinModifierListStubImpl &&
                other.mask == mask
}
