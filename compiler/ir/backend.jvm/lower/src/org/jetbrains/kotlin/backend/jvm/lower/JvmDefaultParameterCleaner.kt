/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterCleaner
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext

@PhaseDescription(
    name = "DefaultParameterCleaner",
    description = "Replace default values arguments with stubs",
    prerequisite = [JvmDefaultArgumentStubGenerator::class]
)
internal class JvmDefaultParameterCleaner(
    context: JvmBackendContext
) : DefaultParameterCleaner(context, replaceDefaultValuesWithStubs = true)
