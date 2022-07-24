/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirCatchImpl(
    override val source: KtSourceElement?,
    override var parameter: FirValueParameter,
    override var block: FirBlock,
) : FirCatch() {
    override val elementKind get() = FirElementKind.Catch

    override fun replaceParameter(newParameter: FirValueParameter) {
        parameter = newParameter
    }

    override fun replaceBlock(newBlock: FirBlock) {
        block = newBlock
    }
}
