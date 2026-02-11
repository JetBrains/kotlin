/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")
object SyntheticCallableId {
    private val syntheticPackageName: FqName = FqName("_synthetic")
    private fun syntheticCallableId(name: String) = CallableId(syntheticPackageName, Name.identifier(name))

    val WHEN = syntheticCallableId("WHEN_CALL")
    val TRY = syntheticCallableId("TRY_CALL")
    val CHECK_NOT_NULL = syntheticCallableId("CHECK_NOT_NULL_CALL")
    val ELVIS = syntheticCallableId("ELVIS_CALL")
    val EQUALITY = syntheticCallableId("EQUALITY_CALL")
    val ID = syntheticCallableId("ID_CALL")
    val ACCEPT_SPECIFIC_TYPE = syntheticCallableId("ACCEPT_SPECIFIC_TYPE_CALL")
}
