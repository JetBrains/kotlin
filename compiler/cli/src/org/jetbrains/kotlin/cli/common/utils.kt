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

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.util.Logger
import java.io.File
import org.jetbrains.kotlin.cli.common.messages.toLogger as toLoggerNew

fun incrementalCompilationIsEnabled(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJvm()
}

fun incrementalCompilationIsEnabledForJs(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJs()
}

fun <F> checkKotlinPackageUsage(
    configuration: CompilerConfiguration,
    files: Collection<F>,
    messageCollector: MessageCollector,
    getPackage: (F) -> FqName,
    getMessageLocation: (F) -> CompilerMessageSourceLocation?,
): Boolean {
    if (configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)) {
        return true
    }
    val kotlinPackage = FqName("kotlin")
    for (file in files) {
        if (getPackage(file).isSubpackageOf(kotlinPackage)) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Only the Kotlin standard library is allowed to use the 'kotlin' package",
                getMessageLocation(file),
            )
            return false
        }
    }
    return true
}

private val CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

fun checkKotlinPackageUsageForPsi(
    configuration: CompilerConfiguration,
    files: Collection<KtFile>,
    messageCollector: MessageCollector = configuration.messageCollector,
) =
    checkKotlinPackageUsage(
        configuration, files, messageCollector,
        getPackage = { it.packageFqName },
        getMessageLocation = { MessageUtil.psiElementToMessageLocation(it.packageDirective!!) },
    )

fun checkKotlinPackageUsageForLightTree(
    configuration: CompilerConfiguration,
    files: Collection<FirFile>,
    messageCollector: MessageCollector = configuration.messageCollector,
) =
    checkKotlinPackageUsage(
        configuration, files, messageCollector,
        getPackage = { it.packageFqName },
        getMessageLocation = { it.packageDirective.source?.getLocationWithin(it) },
    )

private fun KtSourceElement.getLocationWithin(file: FirFile): CompilerMessageLocationWithRange? {
    val sourceFile = file.sourceFile ?: return null
    val (startLine, startColumn) = file.getLineAndColumnStartingWithOnesAt(startOffset) ?: return null
    val (endLine, endColumn) = file.getLineAndColumnStartingWithOnesAt(endOffset) ?: return null
    return CompilerMessageLocationWithRange.create(sourceFile.path, startLine, startColumn, endLine, endColumn, text?.toString())
}

private fun FirFile.getLineAndColumnStartingWithOnesAt(offset: Int?): Pair<Int, Int>? {
    return offset?.let { sourceFileLinesMapping?.getLineAndColumnByOffsetStartingWithOnes(it) }
}

private fun KtSourceFileLinesMapping.getLineAndColumnByOffsetStartingWithOnes(startOffset: Int): Pair<Int, Int> {
    val (line, column) = getLineAndColumnByOffset(startOffset)
    return line + 1 to column + 1
}

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

@Deprecated(
    "Use org.jetbrains.kotlin.cli.common.messages.toLogger() instead",
    ReplaceWith("toLogger()", "org.jetbrains.kotlin.cli.common.messages.toLogger"),
    DeprecationLevel.ERROR
)
fun MessageCollector.toLogger(): Logger = toLoggerNew()
