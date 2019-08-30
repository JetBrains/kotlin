/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.FqName

interface LoggingContext {
    var inVerbosePhase: Boolean
    fun log(message: () -> String)
}

interface CommonBackendContext : BackendContext, LoggingContext {
    override val ir: Ir<CommonBackendContext>

    fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean)

    val configuration: CompilerConfiguration
    val scriptMode: Boolean
}
