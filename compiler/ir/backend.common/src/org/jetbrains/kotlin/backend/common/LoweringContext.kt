/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

/**
 * A context that is used to pass data to both first (before IR serialization) and second (after IR deserialization) stage compiler
 * lowerings.
 */
interface LoweringContext : LoggingContext, ErrorReportingContext {
    val configuration: CompilerConfiguration
    val ir: Ir
    val irBuiltIns: IrBuiltIns
    val irFactory: IrFactory
    val sharedVariablesManager: SharedVariablesManager

    override val messageCollector: MessageCollector
        get() = configuration.messageCollector

    // TODO(KT-73155): Pull this down to CommonBackendContext
    val mapping: Mapping

    fun remapMultiFieldValueClassStructure(
        oldFunction: IrFunction,
        newFunction: IrFunction,
        parametersMappingOrNull: Map<IrValueParameter, IrValueParameter>?,
    ) {
    }
}
