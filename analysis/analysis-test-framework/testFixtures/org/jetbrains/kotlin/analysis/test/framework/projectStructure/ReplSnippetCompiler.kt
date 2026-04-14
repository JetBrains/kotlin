/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

/**
 * Compiles REPL snippets to JAR files for multi-snippet REPL tests.
 *
 * Must be called sequentially for each snippet in order (snippet_001, snippet_002, ...).
 * The implementation maintains REPL history across calls, so each snippet can reference
 * declarations from all previous snippets.
 *
 * @see KtScriptTestModuleFactory
 */
fun interface ReplSnippetCompiler : TestService {
    fun compileSnippetToJar(testModule: TestModule, testServices: TestServices): List<Path>
}

val TestServices.replSnippetCompiler: ReplSnippetCompiler by TestServices.testServiceAccessor()
