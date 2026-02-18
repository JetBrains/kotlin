/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import kotlin.script.experimental.annotations.*
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.isStandalone

/*
 * Those classes use compileOnly dependency on scripting and should not be considered as containing test classes to avoid runtime failures.
 * Thus, they are excluded by utilizing [Test.exclude] in the project buildscript
 */

@KotlinScript(fileExtension = "greet.kts", compilationConfiguration = GreetScriptDefinition::class)
abstract class GreetScriptTemplate {
    fun greet(subject: String) {
        println("Hello, $subject!")
    }
}

@KotlinScript(fileExtension = "greet", compilationConfiguration = GreetScriptDefinition::class)
abstract class GreetScriptCustomExtensionTemplate {
    fun greet(subject: String) {
        println("Hello, $subject!")
    }
}

object GreetScriptDefinition : ScriptCompilationConfiguration(
    {
        isStandalone(false)
    }
)