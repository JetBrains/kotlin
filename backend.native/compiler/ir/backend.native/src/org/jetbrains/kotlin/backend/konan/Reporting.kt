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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getCompilerMessageLocation

internal fun CommonBackendContext.reportCompilationError(message: String, irFile: IrFile, irElement: IrElement): Nothing {
    val location = irElement.getCompilerMessageLocation(irFile)
    this.messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    throw KonanCompilationException()
}

internal fun CommonBackendContext.reportCompilationError(message: String) {
    this.messageCollector.report(CompilerMessageSeverity.ERROR, message, null)
}

internal fun CommonBackendContext.reportCompilationWarning(message: String) {
    this.messageCollector.report(CompilerMessageSeverity.WARNING, message, null)
}
