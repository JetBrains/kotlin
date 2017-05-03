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

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getCompilerMessageLocation

// TODO: declare as a member of BackendContext
val BackendContext.compilerConfiguration: CompilerConfiguration
    get() = when (this) {
        is KonanBackendContext -> this.config.configuration
        is JvmBackendContext -> this.state.configuration
        else -> TODO(this.toString())
    }

val BackendContext.messageCollector: MessageCollector
    get() = this.compilerConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

fun BackendContext.reportWarning(message: String, irFile: IrFile, irElement: IrElement) {
    val location = irElement.getCompilerMessageLocation(irFile)
    this.messageCollector.report(CompilerMessageSeverity.WARNING, message, location)
}

fun <E> MutableList<E>.push(element: E) = this.add(element)

fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

