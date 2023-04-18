/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.Logger
import java.io.File
import kotlin.system.exitProcess

fun incrementalCompilationIsEnabled(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJvm()
}

fun incrementalCompilationIsEnabledForJs(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJs()
}

fun checkKotlinPackageUsage(configuration: CompilerConfiguration, files: Collection<KtFile>, messageCollector: MessageCollector): Boolean {
    if (configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)) {
        return true
    }
    val kotlinPackage = FqName("kotlin")
    for (file in files) {
        if (file.packageFqName.isSubpackageOf(kotlinPackage)) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Only the Kotlin standard library is allowed to use the 'kotlin' package",
                MessageUtil.psiElementToMessageLocation(file.packageDirective!!)
            )
            return false
        }
    }
    return true
}

fun checkKotlinPackageUsage(configuration: CompilerConfiguration, files: Collection<KtFile>): Boolean =
    checkKotlinPackageUsage(configuration, files, configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE))

fun <PathProvider : Any> getLibraryFromHome(
    paths: PathProvider?,
    getLibrary: (PathProvider) -> File,
    libraryName: String,
    messageCollector: MessageCollector,
    noLibraryArgument: String
): File? {
    if (paths != null) {
        val stdlibJar = getLibrary(paths)
        if (stdlibJar.exists()) {
            return stdlibJar
        }
    }

    messageCollector.report(
        CompilerMessageSeverity.STRONG_WARNING, "Unable to find " + libraryName + " in the Kotlin home directory. " +
                "Pass either " + noLibraryArgument + " to prevent adding it to the classpath, " +
                "or the correct '-kotlin-home'", null
    )
    return null
}

fun MessageCollector.toLogger(): Logger =
    object : Logger {
        override fun error(message: String) {
            report(CompilerMessageSeverity.ERROR, message)
        }

        override fun fatal(message: String): Nothing {
            report(CompilerMessageSeverity.ERROR, message)
            exitProcess(1)
        }

        override fun warning(message: String) {
            report(CompilerMessageSeverity.WARNING, message)
        }

        override fun log(message: String) {
            report(CompilerMessageSeverity.LOGGING, message)
        }
    }

