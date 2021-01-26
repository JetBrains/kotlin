/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

abstract class AdditionalSourceProvider(val testServices: TestServices) {
    open val directives: List<DirectivesContainer>
        get() = emptyList()

    /**
     * Note that you can not use [testServices.moduleStructure] here because it's not initialized yet
     */
    abstract fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile>

    protected fun containsDirective(globalDirectives: RegisteredDirectives, module: TestModule, directive: SimpleDirective): Boolean {
        return globalDirectives.contains(directive) || module.directives.contains(directive)
    }

    protected fun File.toTestFile(): TestFile {
        return TestFile(
            this.name,
            this.readText(),
            originalFile = this,
            startLineNumberInOriginalFile = 0,
            isAdditional = true,
            directives = RegisteredDirectives.Empty
        )
    }
}

