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

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.KOTLIN_PACKAGE_USAGE
import org.jetbrains.kotlin.cli.CliDiagnostics.ROOTS_RESOLUTION_WARNING
import org.jetbrains.kotlin.cli.common.arguments.ArgumentField
import org.jetbrains.kotlin.cli.common.arguments.ArgumentLifecycleStatus
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

fun incrementalCompilationIsEnabled(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJvm()
}

fun incrementalCompilationIsEnabledForJs(arguments: CommonCompilerArguments): Boolean {
    return arguments.incrementalCompilation ?: IncrementalCompilation.isEnabledForJs()
}

fun <F> checkKotlinPackageUsage(
    configuration: CompilerConfiguration,
    files: Collection<F>,
    getPackage: (F) -> FqName,
    getMessageLocation: (F) -> CompilerMessageSourceLocation?,
): Boolean {
    if (configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)) {
        return true
    }
    val kotlinPackage = FqName("kotlin")
    for (file in files) {
        if (getPackage(file).isSubpackageOf(kotlinPackage)) {
            configuration.report(
                KOTLIN_PACKAGE_USAGE,
                "Only the Kotlin standard library is allowed to use the 'kotlin' package",
                getMessageLocation(file),
            )
            return false
        }
    }
    return true
}

fun checkKotlinPackageUsageForPsi(
    configuration: CompilerConfiguration,
    files: Collection<KtFile>,
): Boolean {
    return checkKotlinPackageUsage(
        configuration, files,
        getPackage = { it.packageFqName },
        getMessageLocation = { MessageUtil.psiElementToMessageLocation(it.packageDirective!!) },
    )
}

fun <PathProvider : Any> getLibraryFromHome(
    paths: PathProvider?,
    getLibrary: (PathProvider) -> File,
    libraryName: String,
    configuration: CompilerConfiguration,
    noLibraryArgument: String
): File? {
    if (paths != null) {
        val stdlibJar = getLibrary(paths)
        if (stdlibJar.exists()) {
            return stdlibJar
        }
    }

    configuration.report(
        ROOTS_RESOLUTION_WARNING,
        "Unable to find " + libraryName + " in the Kotlin home directory. " +
                "Pass either " + noLibraryArgument + " to prevent adding it to the classpath, " +
                "or the correct '-kotlin-home'", null
    )
    return null
}

fun createPerformanceManagerFor(platform: TargetPlatform) =
    PerformanceManagerImpl(platform, "Kotlin to ${if (platform.isCommon()) "Metadata" else platform.first().platformName} compiler")

fun disposeRootInWriteAction(disposable: Disposable) {
    if (ApplicationManager.getApplication() != null) {
        runWriteAction {
            Disposer.dispose(disposable)
        }
    } else {
        Disposer.dispose(disposable)
    }
}

fun CompilerConfiguration.reportCompilationException(e: CompilationException) {
    report(
        CliDiagnostics.COMPILER_EXCEPTION,
        e.stackTraceToString(),
        CompilerMessageLocation.create(
            path = e.path,
            line = e.line,
            column = e.column,
            lineContent = e.content
        )
    )
}

/**
 * Returns a warning message and status if the argument is deprecated or removed, null otherwise.
 */
internal fun ArgumentField.generateLifecycleWarning(forExtraHelp: Boolean): Pair<String, ArgumentLifecycleStatus>? {
    val status = status
    if (status == ArgumentLifecycleStatus.REGULAR ||
        // Don't report a diagnostic about future deprecation/removing
        !forExtraHelp && (status == ArgumentLifecycleStatus.WILL_BE_DEPRECATED || status == ArgumentLifecycleStatus.WILL_BE_REMOVED) ||
        // It doesn't make sense to show a warning for removed arguments in extra help
        forExtraHelp && status >= ArgumentLifecycleStatus.REMOVED
    ) {
        return null
    }

    val argument = argument
    val message = buildString {
        if (forExtraHelp) {
            append("The option ")
        } else {
            append("The argument '")
            append(argument.value)
            append("' ")
        }

        when (status) {
            ArgumentLifecycleStatus.REGULAR -> shouldNotBeCalled()
            ArgumentLifecycleStatus.WILL_BE_DEPRECATED,
            ArgumentLifecycleStatus.WILL_BE_REMOVED -> {
                append("will be ")
                append(if (status == ArgumentLifecycleStatus.WILL_BE_DEPRECATED) "deprecated" else "removed")
                append(" in ")
                append(if (status == ArgumentLifecycleStatus.WILL_BE_DEPRECATED) argument.deprecatedVersion else argument.removedVersion)
                append('.')
            }
            ArgumentLifecycleStatus.DEPRECATED,
            ArgumentLifecycleStatus.DEPRECATED_AND_WILL_BE_REMOVED,
                -> {
                append("is deprecated since Kotlin ")
                append(argument.deprecatedVersion)
                append(". It will be removed in ")
                if (status == ArgumentLifecycleStatus.DEPRECATED) {
                    append("one of the future releases")
                } else {
                    append(argument.removedVersion)
                }
                append('.')
                deprecatedAnnotation?.message?.takeIf { it.isNotEmpty() }?.let {
                    append(' ')
                    append(it)
                }
            }
            ArgumentLifecycleStatus.REMOVED -> {
                append("was removed in Kotlin ")
                append(argument.removedVersion)
                append(". It has no effect.")
            }
        }
    }

    return message to status
}
