/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.ir.declarations.IrClass

class Fir2IrLocalClassStorage(private val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()) {
    operator fun get(localClass: FirClass): IrClass? {
        return localClassCache[localClass]
    }

    operator fun set(firClass: FirClass, irClass: IrClass) {
        localClassCache[firClass] = irClass
    }
}