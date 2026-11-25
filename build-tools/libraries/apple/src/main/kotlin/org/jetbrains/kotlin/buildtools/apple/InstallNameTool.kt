/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/** `install_name_tool`, which edits load commands of Mach-O binaries. */
public class InstallNameTool internal constructor(private val tools: AppleTools) {

    /** `install_name_tool -id <id> <binary>`, for example with `@rpath/Shared.framework/Shared` as [id]. */
    public fun setIdInvocation(binary: Path, id: String): AppleToolInvocation {
        require(id.isNotBlank()) { "The install name must not be blank" }
        return tools.toolchainInvocation("install_name_tool", listOf("-id", id, binary.toString()))
    }

    /** Runs [setIdInvocation]. */
    public fun setId(binary: Path, id: String) {
        tools.runner.runChecked(setIdInvocation(binary, id))
    }
}
