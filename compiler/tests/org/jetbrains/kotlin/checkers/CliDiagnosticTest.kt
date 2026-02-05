/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator
import org.jetbrains.kotlin.cli.common.arguments.checkApiAndLanguageVersion
import org.jetbrains.kotlin.cli.common.fromConfiguration
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.utils.checkRules
import org.junit.Assert
import org.junit.Test

class CliDiagnosticTest {
    @Test
    fun verify() {
        val collector = MessageCollectorStub()
        val configuration = CompilerConfiguration.create(messageCollector = collector)
        val apiVersions = LanguageVersion.entries.map { ApiVersion.createByLanguageVersion(it) }
        for (languageVersion in LanguageVersion.entries) {
            for (apiVersion in apiVersions) {
                for (argumentVariant in argumentVariants) {
                    argumentVariant.checkApiAndLanguageVersion(
                        languageVersion,
                        apiVersion,
                        CommonCompilerArgumentsConfigurator.Reporter.fromConfiguration(configuration)
                    )
                }
            }
        }
        val errors = mutableListOf<String>()
        for (message in collector.messages) {
            errors.checkRules("language/API version correctness", message, 0)
        }
        if (errors.isNotEmpty()) {
            Assert.fail(
                errors.joinToString(
                    "\n\n",
                    postfix = "\n\nSee https://youtrack.jetbrains.com/articles/KT-A-610 for the style guide.\n\n"
                )
            )
        }
    }

    private class MessageCollectorStub : MessageCollector {
        val messages = mutableListOf<String>()

        override fun clear() {}

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            messages += message
        }

        override fun hasErrors(): Boolean = false
    }

    private val argumentVariants = buildList<CommonCompilerArguments> {
        val v1 = CommonCompilerArgumentsStub().apply {
            suppressApiVersionGreaterThanLanguageVersionError = false
            suppressVersionWarnings = false
            progressiveMode = true
        }
        val v2 = CommonCompilerArgumentsStub().apply {
            suppressApiVersionGreaterThanLanguageVersionError = true
            suppressVersionWarnings = false
        }
        val v3 = CommonCompilerArgumentsStub().apply {
            suppressApiVersionGreaterThanLanguageVersionError = false
            suppressVersionWarnings = true
        }
        add(v1)
        add(v2)
        add(v3)
    }

    private class CommonCompilerArgumentsStub() : CommonCompilerArguments() {
        override val configurator: CommonCompilerArgumentsConfigurator
            get() = TODO("Not yet implemented")
    }
}
