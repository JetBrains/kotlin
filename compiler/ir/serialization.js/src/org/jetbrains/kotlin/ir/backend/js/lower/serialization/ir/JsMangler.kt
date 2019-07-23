/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.KotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInlined

object JsMangler : KotlinManglerImpl() {
    private const val MOD_VALUE = PUBLIC_LOCAL_UNIQ_ID_EDGE

    override val String.hashMangle: Long get() = cityHash64() % MOD_VALUE

    override val IrType.isInlined: Boolean
        get() = this.isInlined()
}

