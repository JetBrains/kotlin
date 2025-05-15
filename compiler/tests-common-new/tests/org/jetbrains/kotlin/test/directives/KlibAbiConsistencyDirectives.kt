/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object KlibAbiConsistencyDirectives : SimpleDirectivesContainer() {
    val CHECK_SAME_ABI_AFTER_INLINING by directive(
        description = "Enable checking if ABI is the same before and after inlining"
    )
}
