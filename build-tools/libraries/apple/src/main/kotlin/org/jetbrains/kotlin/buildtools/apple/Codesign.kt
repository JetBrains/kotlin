/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/** `/usr/bin/codesign`. */
public class Codesign internal constructor(private val tools: AppleTools) {

    /**
     * `codesign [--force] --sign <identity> -- <file>`.
     *
     * @param identity a signing identity name or hash, or `-` for ad-hoc signing.
     * @param force replace an existing signature.
     */
    public fun signInvocation(file: Path, identity: String, force: Boolean = true): AppleToolInvocation {
        require(identity.isNotBlank()) { "The signing identity must not be blank" }
        val arguments = buildList {
            if (force) add("--force")
            add("--sign"); add(identity)
            add("--"); add(file.toString())
        }
        return tools.systemInvocation(CODESIGN, arguments)
    }

    /** Runs [signInvocation]. */
    public fun sign(file: Path, identity: String, force: Boolean = true) {
        tools.runner.runChecked(signInvocation(file, identity, force))
    }

    private companion object {
        val CODESIGN: Path = Paths.get("/usr/bin/codesign")
    }
}
