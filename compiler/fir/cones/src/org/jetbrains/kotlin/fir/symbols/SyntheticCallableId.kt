/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SyntheticCallableId {
    private val syntheticPackageName: FqName = FqName("_synthetic")

    val WHEN = CallableId(
        syntheticPackageName,
        Name.identifier("WHEN_CALL")
    )
    val TRY = CallableId(
        syntheticPackageName,
        Name.identifier("TRY_CALL")
    )
    val CHECK_NOT_NULL = CallableId(
        syntheticPackageName,
        Name.identifier("CHECK_NOT_NULL_CALL")
    )

    val ELVIS_NOT_NULL = CallableId(
        syntheticPackageName,
        Name.identifier("ELVIS_CALL")
    )

    val ID = CallableId(
        syntheticPackageName,
        Name.identifier("ID_CALL")
    )

    val ACCEPT_SPECIFIC_TYPE = CallableId(
        syntheticPackageName,
        Name.identifier("ACCEPT_SPECIFIC_TYPE_CALL")
    )
}
