/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.js.config.EcmaVersion

val K2JSCompilerArguments.targetVersion: EcmaVersion?
    get() {
        val targetString = target
        return when {
            targetString != null -> EcmaVersion.fromName(targetString)
            else -> EcmaVersion.defaultVersion()
        }
    }
