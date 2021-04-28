/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global

object FirIdeDirectives : SimpleDirectivesContainer() {
    val FIR_IDE_IGNORE by directive(
        description = "Test is ignored in FIR IDE",
        applicability = Global
    )
}