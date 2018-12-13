/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile

internal fun CommonBackendContext.reportCompilationError(message: String, irFile: IrFile, irElement: IrElement): Nothing {
    report(irElement, irFile, message, true)
    throw KonanCompilationException()
}

internal fun CommonBackendContext.reportCompilationError(message: String): Nothing {
    report(null, null, message, true)
    throw KonanCompilationException()
}

internal fun CommonBackendContext.reportCompilationWarning(message: String) {
    report(null, null, message, false)
}
