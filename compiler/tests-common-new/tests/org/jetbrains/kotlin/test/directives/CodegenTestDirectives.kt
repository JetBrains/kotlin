/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CodegenTestDirectives : SimpleDirectivesContainer() {
    val IGNORE_BACKEND by enumDirective<TargetBackend>(
        description = "Ignore failures of test on target backend"
    )
}
