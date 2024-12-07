/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.PlainTextMessageRenderer
import org.jetbrains.kotlin.test.model.TestModule

/**
 * Rewrites the paths of the IR validation error messages to point to the original test files instead of
 * virtual ones declared using the `// FILE:` directive to ease investigating such errors from the IDE.
 */
class CompilerTestMessageRenderer(private val module: TestModule) : PlainTextMessageRenderer() {

    override fun getPath(location: CompilerMessageSourceLocation): String {
        // Relative paths in IrFiles start with a forward slash, while paths written in TestFiles don't
        val sanitizedDiagnosticPath = location.path.removePrefix("/")
        return module
            .files
            .find { it.relativePath == sanitizedDiagnosticPath }
            ?.originalFile
            ?.absoluteFile
            ?.toPath()
            ?.toUri()
            ?.toString()
            ?: location.path
    }

    override fun getName(): String = this.javaClass.simpleName
}
