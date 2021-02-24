/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.CallableId

interface SyntheticSymbol

interface AccessorSymbol {
    val callableId: CallableId // it's an id of related property (synthetic or not)
    val accessorId: CallableId // it's an id of accessor function
}