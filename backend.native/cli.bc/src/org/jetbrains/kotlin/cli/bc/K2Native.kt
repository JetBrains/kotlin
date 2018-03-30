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

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.util.profile
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.KotlinPaths

class K2Native : CLICompiler<K2NativeCompilerArguments>() {

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KonanVersion.CURRENT}")
            return ExitCode.OK
        }

        if (arguments.freeArgs.isEmpty() && !arguments.isUsefulWithoutFreeArgs) {
            configuration.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)
        val project = environment.project
        val konanConfig = KonanConfig(project, configuration)

        try {
            runTopLevelPhases(konanConfig, environment)
        } catch (e: KonanCompilationException) {
            return ExitCode.COMPILATION_ERROR
        } catch (e: Throwable) {
            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version info: Konan: ${KonanVersion.CURRENT} / Kotlin: ${KotlinVersion.CURRENT}
                | * Output kind: ${configuration.get(KonanConfigKeys.PRODUCE)}

                """.trimMargin())
            throw e
        }

        return ExitCode.OK
    }

    val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() = this.listTargets || this.listPhases || this.checkDependencies

    fun Array<String>?.toNonNullList(): List<String> {
        return this?.asList<String>() ?: listOf<String>()
    }

    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {

        configuration.addKotlinSourceRoots(arguments.freeArgs)

        with(KonanConfigKeys) {
            with(configuration) {

                put(NODEFAULTLIBS, arguments.nodefaultlibs)
                put(NOSTDLIB, arguments.nostdlib)
                put(NOPACK, arguments.nopack)
                put(NOMAIN, arguments.nomain)
                put(LIBRARY_FILES,
                        arguments.libraries.toNonNullList())

                put(LINKER_ARGS, arguments.linkerArguments.toNonNullList())
                arguments.moduleName ?. let{ put(MODULE_NAME, it) }
                arguments.target ?.let{ put(TARGET, it) }

                put(INCLUDED_BINARY_FILES,
                        arguments.includeBinaries.toNonNullList())
                put(NATIVE_LIBRARY_FILES,
                        arguments.nativeLibraries.toNonNullList())
                put(REPOSITORIES,
                        arguments.repositories.toNonNullList())

                // TODO: Collect all the explicit file names into an object
                // and teach the compiler to work with temporaries and -save-temps.

                arguments.outputName ?.let { put(OUTPUT, it) } 
                val outputKind = CompilerOutputKind.valueOf(
                    (arguments.produce ?: "program").toUpperCase())
                put(PRODUCE, outputKind)
                put(ABI_VERSION, 1)

                arguments.mainPackage ?.let{ put(ENTRY, it) }
                arguments.manifestFile ?.let{ put(MANIFEST_FILE, it) }
                arguments.runtimeFile ?.let{ put(RUNTIME_FILE, it) }
                arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }

                put(LIST_TARGETS, arguments.listTargets)
                put(OPTIMIZATION, arguments.optimization)
                put(DEBUG, arguments.debug)

                put(PRINT_IR, arguments.printIr)
                put(PRINT_IR_WITH_DESCRIPTORS, arguments.printIrWithDescriptors)
                put(PRINT_DESCRIPTORS, arguments.printDescriptors)
                put(PRINT_LOCATIONS, arguments.printLocations)
                put(PRINT_BITCODE, arguments.printBitCode)

                put(PURGE_USER_LIBS, arguments.purgeUserLibs)

                put(VERIFY_IR, arguments.verifyIr)
                put(VERIFY_DESCRIPTORS, arguments.verifyDescriptors)
                put(VERIFY_BITCODE, arguments.verifyBitCode)

                put(ENABLED_PHASES,
                        arguments.enablePhases.toNonNullList())
                put(DISABLED_PHASES,
                        arguments.disablePhases.toNonNullList())
                put(VERBOSE_PHASES,
                        arguments.verbosePhases.toNonNullList())
                put(LIST_PHASES, arguments.listPhases)
                put(TIME_PHASES, arguments.timePhases)

                put(ENABLE_ASSERTIONS, arguments.enableAssertions)

                put(GENERATE_TEST_RUNNER, arguments.generateTestRunner)

                // We need to download dependencies only if we use them ( = there are files to compile).
                put(CHECK_DEPENDENCIES, if (configuration.kotlinSourceRoots.isNotEmpty()) {
                        true
                    } else {
                        arguments.checkDependencies
                    })
            }
        }
    }

    override fun createArguments(): K2NativeCompilerArguments {
        return K2NativeCompilerArguments().apply { coroutinesState = "enable" }
    }

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                val options = args.flatMap {
                    if (it.startsWith('@')) {
                        File(it.substring(1)).readStrings()
                    }
                    else listOf(it)
                }
                CLITool.doMain(K2Native(), options.toTypedArray())
            }
        }
    }
}
fun main(args: Array<String>) = K2Native.main(args)

