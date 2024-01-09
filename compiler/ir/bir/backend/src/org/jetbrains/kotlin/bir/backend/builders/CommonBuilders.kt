/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

fun createBirDispatchReceiver() = BirValueParameter.build {
    index = -1
    name = "this".synthesizedName
}

fun createBirExtensionReceiver() = BirValueParameter.build {
    index = -1
    name = "receiver".synthesizedName
    origin = IrDeclarationOrigin.DEFINED
}