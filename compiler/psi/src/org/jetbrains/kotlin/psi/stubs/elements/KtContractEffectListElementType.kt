/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtContractEffectList

class KtContractEffectListElementType(debugName: String) :
    KtPlaceHolderStubElementType<KtContractEffectList>(debugName, KtContractEffectList::class.java) {
}