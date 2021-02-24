/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.util.KotlinMangler

abstract class AbstractKotlinMangler<D : Any> : KotlinMangler<D> {
    override val String.hashMangle get() = cityHash64()

    abstract fun getExportChecker(): KotlinExportChecker<D>
    abstract fun getMangleComputer(mode: MangleMode): KotlinMangleComputer<D>
}