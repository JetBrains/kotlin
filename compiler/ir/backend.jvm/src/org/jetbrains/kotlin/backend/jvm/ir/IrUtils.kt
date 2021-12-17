/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunctionTypeOrSubtype

// this function moved to the IrUtils.kt file to restore binary compatibility with rhizomedb & noria
fun IrValueParameter.isInlineParameter() =
    index >= 0 && !isNoinline && (type.isFunction() || type.isSuspendFunctionTypeOrSubtype()) &&
            // Parameters with default values are always nullable, so check the expression too.
            // Note that the frontend has a diagnostic for nullable inline parameters, so actually
            // making this return `false` requires using `@Suppress`.
            (!type.isNullable() || defaultValue?.expression?.type?.isNullable() == false)
