/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.TAILREC_MODIFIER
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrFunction

object CommonBackendErrors : KtDiagnosticsContainer() {
    val INLINE_CALL_CYCLE by error1<IrFunction>()

    val IR_DUMP_WARNING by warningWithoutSource()

    val NON_TAIL_RECURSIVE_CALL by warning0(REFERENCED_NAME_BY_QUALIFIED)

    val NO_TAIL_CALLS_FOUND by warning0(TAILREC_MODIFIER)

    val TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED by warning0(REFERENCED_NAME_BY_QUALIFIED)

    val TAILREC_ON_VIRTUAL_MEMBER_ERROR by error0(TAILREC_MODIFIER)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultCommonBackendErrorMessages
    }
}

object KtDefaultCommonBackendErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            CommonBackendErrors.INLINE_CALL_CYCLE,
            "The ''{0}'' invocation is part of an inline cycle.",
            IrDiagnosticRenderers.DECLARATION_NAME,
        )
        map.put(
            CommonBackendErrors.IR_DUMP_WARNING,
            "{0}"
        )
        map.put(
            CommonBackendErrors.NON_TAIL_RECURSIVE_CALL,
            "Recursive call is not a tail call.",
        )
        map.put(
            CommonBackendErrors.NO_TAIL_CALLS_FOUND,
            "A function is marked as tail-recursive but no tail calls are found.",
        )
        map.put(
            CommonBackendErrors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED,
            "Tail recursion optimization inside try/catch/finally is not supported.",
        )
        map.put(
            CommonBackendErrors.TAILREC_ON_VIRTUAL_MEMBER_ERROR,
            "Tailrec is prohibited on open and abstract members.",
        )
    }
}
