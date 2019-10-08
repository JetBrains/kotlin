/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SyntheticCallableId {
    val WHEN = CallableId(
        FqName("_synthetic"),
        Name.identifier("WHEN_CALL")
    )
    val TRY = CallableId(
        FqName("_synthetic"),
        Name.identifier("TRY_CALL")
    )
}