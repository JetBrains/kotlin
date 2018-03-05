/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.daemon.common.CompileService

public interface CompilerSelector {
    operator fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*>

    companion object {
        fun getDefault() = object : CompilerSelector {
            override operator fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = TODO("not implemented")
        }
    }
}