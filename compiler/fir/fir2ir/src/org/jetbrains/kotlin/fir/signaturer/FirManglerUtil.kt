/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

val FirCallableDeclaration.irName: Name
    get() = when (this) {
        is FirConstructor -> SpecialNames.INIT
        is FirSimpleFunction -> this.name
        is FirVariable -> this.name
        else -> SpecialNames.ANONYMOUS
    }
