/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.InventNamesForLocalFunctions
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.name.NameUtils.sanitizeAsJavaIdentifier

@PhaseDescription(
    name = "InventNamesForLocalClasses",
    prerequisite = [
        // The tailrec lowering copies the default arguments into the lowered function body.
        // If such an argument is a lambda, a new local function will appear in the lowered body, which needs a new name.
        JvmTailrecLowering::class,
    ],
)
internal class JvmInventNamesForLocalFunctions(
    private val context: JvmBackendContext
) : InventNamesForLocalFunctions() {
    override val suggestUniqueNames get() = true
    override val compatibilityModeForInlinedLocalDelegatedPropertyAccessors get() = true

    override fun sanitizeNameIfNeeded(name: String) = sanitizeAsJavaIdentifier(name)
}
