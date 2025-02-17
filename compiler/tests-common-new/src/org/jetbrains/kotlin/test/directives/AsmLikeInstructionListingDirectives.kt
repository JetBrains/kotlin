/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.backend.handlers.AsmLikeInstructionListingHandler
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object AsmLikeInstructionListingDirectives : SimpleDirectivesContainer() {
    val CHECK_ASM_LIKE_INSTRUCTIONS by directive(
        "Enables ${AsmLikeInstructionListingHandler::class}"
    )

    val INLINE_SCOPES_DIFFERENCE by directive(
        "If present and if inline scopes are enabled then saves dump for IR backend in asm.scopes.txt file"
    )

    val FIR_DIFFERENCE by directive(
        "If present then saves dump for IR backend in asm.fir.txt file"
    )

    val CURIOUS_ABOUT by stringDirective(
        "Specifies list of methods for which asm instructions should be printed"
    )

    val LOCAL_VARIABLE_TABLE by directive(
        "Enables printing of local variable table"
    )

    val RENDER_ANNOTATIONS by directive(
        "Enables rendering of annotations"
    )
}
