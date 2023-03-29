/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MemoizedValueClassLoweringDispatcherSharedData {
    enum class Access { Header, Body }

    val functionResults: ConcurrentMap<Pair<Access, IrFunction>, Boolean> = ConcurrentHashMap()
    val classResults: ConcurrentMap<Pair<Access, IrClass>, Boolean> = ConcurrentHashMap()
    val fieldResults: ConcurrentMap<Pair<Access, IrField>, Boolean> = ConcurrentHashMap()
}
