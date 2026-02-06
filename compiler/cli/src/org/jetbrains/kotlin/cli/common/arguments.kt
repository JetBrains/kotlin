/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_WARNING
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.FlexibleTypeImpl
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun CompilerConfiguration.setupCommonArguments(
    arguments: CommonCompilerArguments,
    createMetadataVersion: ((IntArray) -> BinaryVersion)? = null
) {
    put(CommonConfigurationKeys.DISABLE_INLINE, arguments.noInline)
    put(CommonConfigurationKeys.USE_FIR_EXTRA_CHECKERS, arguments.extraWarnings)
    put(CommonConfigurationKeys.METADATA_KLIB, arguments.metadataKlib)

    // Important! Uncomment the reading from the environment below only for non-public builds, the environment reading should not be part of any public release.
    val modelDumpDirString = arguments.dumpArgumentsDir // ?: System.getenv("KOTLIN_DUMP_MODEL")
    val modelDumpDir = modelDumpDirString?.takeIf { it.isNotEmpty() && File(it).let { it.isDirectory && it.canWrite() } }

    putIfNotNull(CommonConfigurationKeys.DUMP_MODEL, modelDumpDir)
    putIfNotNull(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, arguments.intellijPluginRoot)
    put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, arguments.reportOutputFiles)
    put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, incrementalCompilationIsEnabled(arguments))
    put(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, arguments.allowAnyScriptsInSourceRoots)
    put(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, arguments.ignoreConstOptimizationErrors)
    put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

    val irVerificationMode = arguments.verifyIr?.let { verifyIrString ->
        IrVerificationMode.resolveMode(verifyIrString).also {
            if (it == null) {
                this.report(COMPILER_ARGUMENTS_ERROR, "Unsupported IR verification mode $verifyIrString")
            }
        }
    } ?: IrVerificationMode.NONE
    put(CommonConfigurationKeys.VERIFY_IR, irVerificationMode)

    if (arguments.verifyIrVisibility) {
        put(CommonConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS, true)
        if (irVerificationMode == IrVerificationMode.NONE) {
            this.report(
                COMPILER_ARGUMENTS_WARNING,
                "'-Xverify-ir-visibility' has no effect unless '-Xverify-ir=warning' or '-Xverify-ir=error' is specified"
            )
        }
    }

    if (arguments.verifyIrNestedOffsets) {
        put(CommonConfigurationKeys.ENABLE_IR_NESTED_OFFSETS_CHECKS, true)
        if (irVerificationMode == IrVerificationMode.NONE) {
            this.report(
                COMPILER_ARGUMENTS_WARNING,
                "'-Xverify-ir-nested-offsets' has no effect unless '-Xverify-ir=warning' or '-Xverify-ir=error' is specified"
            )
        }
    }

    @Suppress("DEPRECATION")
    if (arguments.useFirExperimentalCheckers) {
        put(CommonConfigurationKeys.USE_FIR_EXPERIMENTAL_CHECKERS, true)
        this.report(
            COMPILER_ARGUMENTS_WARNING,
            "'-Xuse-fir-experimental-checkers' is deprecated and will be removed in a future release"
        )
    }

    setupMetadataVersion(arguments, createMetadataVersion)

    setupLanguageVersionSettings(arguments)

    // It should be called after the language version is initialized because the reporting depends on the current language version
    checkRedundantArguments(arguments)

    val usesK2 = languageVersionSettings.languageVersion.usesK2
    put(CommonConfigurationKeys.USE_FIR, usesK2)
    put(CommonConfigurationKeys.USE_LIGHT_TREE, arguments.useFirLT)
    buildHmppModuleStructure(arguments)?.let { put(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE, it) }

    if (arguments.debugLevelCompilerChecks) {
        FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    put(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES, arguments.dontSortSourceFiles)
}

fun CompilerConfiguration.setupMetadataVersion(
    arguments: CommonCompilerArguments,
    createMetadataVersion: ((IntArray) -> BinaryVersion)?,
) {
    val metadataVersionString = arguments.metadataVersion
    if (metadataVersionString != null) {
        val versionArray = BinaryVersion.parseVersionArray(metadataVersionString)
        when {
            versionArray == null -> this.report(
                COMPILER_ARGUMENTS_ERROR, "Invalid metadata version: $metadataVersionString", null
            )
            createMetadataVersion == null -> throw IllegalStateException("Unable to create metadata version: missing argument")
            else -> put(CommonConfigurationKeys.METADATA_VERSION, createMetadataVersion(versionArray))
        }
    }
}
fun CommonCompilerArgumentsConfigurator.Reporter.Companion.fromConfiguration(configuration: CompilerConfiguration): CommonCompilerArgumentsConfigurator.Reporter {
    return object : CommonCompilerArgumentsConfigurator.Reporter {
        override fun reportWarning(message: String) {
            configuration.report(COMPILER_ARGUMENTS_WARNING, message)
        }

        override fun reportError(message: String) {
            configuration.report(COMPILER_ARGUMENTS_ERROR, message)
        }

        override fun info(message: String) {
            configuration.reportInfo(message)
        }
    }
}

fun CompilerConfiguration.setupLanguageVersionSettings(arguments: CommonCompilerArguments) {
    val reporter = CommonCompilerArgumentsConfigurator.Reporter.fromConfiguration(this)
    languageVersionSettings = arguments.toLanguageVersionSettings(reporter)
}

private fun CompilerConfiguration.checkRedundantArguments(arguments: CommonCompilerArguments) {
    val languageVersion = languageVersionSettings.languageVersion

    propertiesLoop@ for ((explicitArgument, values) in arguments.explicitArguments) {
        if (!explicitArgument.changesLanguageFeatures) continue@propertiesLoop
        val effectivePropertyValue = values.lastOrNull() ?: continue@propertiesLoop

        fun checkNecessity(feature: LanguageFeature, ifValueIs: String, state: LanguageFeature.State): Boolean {
            // At first, check if the annotation is relevant. Only Boolean and String types are allowed
            when {
                // Language features can't be disabled, so it's expected if the value is changed, it's always `true`
                ifValueIs.isEmpty() -> require(effectivePropertyValue as Boolean)
                else -> if (effectivePropertyValue as String != ifValueIs) return false
            }

            // At second check the necessity
            return (state == LanguageFeature.State.ENABLED) != languageVersionSettings.isEnabledByDefault(feature)
        }

        explicitArgument.enablesAnnotations.forEach {
            if (checkNecessity(it.feature, it.ifValueIs, LanguageFeature.State.ENABLED)) continue@propertiesLoop
        }
        explicitArgument.disablesAnnotations.forEach {
            if (checkNecessity(it.feature, it.ifValueIs, LanguageFeature.State.DISABLED)) continue@propertiesLoop
        }

        val argValue = if (effectivePropertyValue is String) "=$effectivePropertyValue" else ""
        this.report(
            CliDiagnostics.REDUNDANT_CLI_ARG,
            "The argument '${explicitArgument.argument.value}${argValue}' is redundant for the current language version $languageVersion.",
        )
    }
}

const val KOTLIN_HOME_PROPERTY = "kotlin.home"

fun computeKotlinPaths(configuration: CompilerConfiguration, arguments: CommonCompilerArguments): KotlinPaths? {
    val kotlinHomeProperty = System.getProperty(KOTLIN_HOME_PROPERTY)
    val kotlinHome = when {
        arguments.kotlinHome != null -> File(arguments.kotlinHome!!)
        kotlinHomeProperty != null -> File(kotlinHomeProperty)
        else -> null
    }

    return when {
        kotlinHome == null -> PathUtil.kotlinPathsForCompiler
        kotlinHome.isDirectory -> KotlinPathsFromHomeDir(kotlinHome)
        else -> {
            configuration.report(COMPILER_ARGUMENTS_ERROR, "Kotlin home does not exist or is not a directory: $kotlinHome")
            null
        }
    }?.also {
        configuration.reportLog("Using Kotlin home directory " + it.homePath, null)
    }
}

fun MessageCollector.reportArgumentParseProblems(arguments: CommonToolArguments) {
    for ((key, values) in arguments.explicitArguments) {
        if (values.size <= 1 || values.distinct().size == 1) continue

        val argName = key.argument.value
        val valuesString = values.joinToString("', '")
        val message = "Argument '$argName' is passed multiple times: '$valuesString'. The last value will be used."
        report(CompilerMessageSeverity.STRONG_WARNING, message)
    }

    val errors = arguments.errors ?: return
    for (flag in errors.unknownExtraFlags) {
        report(CompilerMessageSeverity.STRONG_WARNING, "Flag is not supported by this version of the compiler: $flag")
    }
    for (argument in errors.extraArgumentsPassedInObsoleteForm) {
        report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Advanced option value is passed in an obsolete form. Please use the '=' character to specify the value: $argument=..."
        )
    }
    for ((deprecatedName, newName) in errors.deprecatedArguments) {
        report(CompilerMessageSeverity.STRONG_WARNING, "Argument $deprecatedName is deprecated. Please use $newName instead")
    }
    for (argfileError in errors.argfileErrors) {
        report(CompilerMessageSeverity.STRONG_WARNING, argfileError)
    }

    reportUnsafeInternalArgumentsIfAny(arguments)

    for ((severity, internalArgumentsProblem) in errors.internalArgumentsParsingProblems) {
        report(severity, internalArgumentsProblem)
    }
}

private fun MessageCollector.reportUnsafeInternalArgumentsIfAny(arguments: CommonToolArguments) {
    val unsafeArguments = arguments.internalArguments.filterNot {
        // -XXLanguage which turns on BUG_FIX considered safe
        it.languageFeature.actuallyEnabledInProgressiveMode && it.state == LanguageFeature.State.ENABLED
    }

    if (unsafeArguments.isNotEmpty()) {
        val unsafeArgumentsString = unsafeArguments.joinToString(prefix = "\n", postfix = "\n\n", separator = "\n") {
            it.stringRepresentation
        }

        report(
            CompilerMessageSeverity.STRONG_WARNING,
            "ATTENTION!\n" +
                    "This build uses unsafe internal compiler arguments:\n" +
                    unsafeArgumentsString +
                    "This mode is not recommended for production use,\n" +
                    "as no stability/compatibility guarantees are given on\n" +
                    "compiler or generated code. Use it at your own risk!\n"
        )
    }
}

private val FRAGMENTS_ARG_NAME = CommonCompilerArguments::fragments.cliArgument
private val FRAGMENT_REFINES_ARG_NAME = CommonCompilerArguments::fragmentRefines.cliArgument
private val FRAGMENT_SOURCES_ARG_NAME = CommonCompilerArguments::fragmentSources.cliArgument
private val FRAGMENT_DEPENDENCIES_ARG_NAME = CommonCompilerArguments::fragmentDependencies.cliArgument
private val FRAGMENT_FRIEND_DEPENDENCIES_ARG_NAME = CommonCompilerArguments::fragmentFriendDependencies.cliArgument

private fun CompilerConfiguration.buildHmppModuleStructure(arguments: CommonCompilerArguments): HmppCliModuleStructure? {
    val rawFragments = arguments.fragments
    val rawFragmentSources = arguments.fragmentSources
    val rawFragmentRefines = arguments.fragmentRefines

    fun reportError(message: String) {
        this.report(COMPILER_ARGUMENTS_ERROR, message)
    }

    fun reportWarning(message: String) {
        this.report(COMPILER_ARGUMENTS_WARNING, message)
    }

    if (rawFragments == null) {
        if (rawFragmentRefines != null) {
            reportError("$FRAGMENT_REFINES_ARG_NAME flag can not be used without $FRAGMENTS_ARG_NAME")
        }
        return null
    }

    if (!languageVersionSettings.languageVersion.usesK2) {
        reportWarning("$FRAGMENTS_ARG_NAME flag is not supported for language version < 2.0")
        return null
    }


    val sourcesByFragmentName: Map<String, Set<String>> = rawFragments.associateWith { mutableSetOf<String>() }.apply {
        rawFragmentSources.orEmpty().forEach { rawFragmentSourceArg ->
            val split = rawFragmentSourceArg.split(":", limit = 2)
            if (split.size < 2) {
                reportError(
                    "Incorrect syntax for $FRAGMENT_SOURCES_ARG_NAME argument. " +
                            "`<module name>:<source file>` expected but got `$rawFragmentSourceArg`"
                )
                return@forEach
            }
            val fragmentName = split[0]
            val fragmentSource = split[1]

            getOrElse(fragmentName) {
                reportError(
                    "Passed $rawFragmentSourceArg, " +
                            "but fragment `$fragmentName` of source file $fragmentSource is not specified in $FRAGMENTS_ARG_NAME"
                )
                return@forEach
            }.add(fragmentSource)
        }
    }

    var modules = sourcesByFragmentName.map { (fragmentName, sources) -> HmppCliModule(fragmentName, sources) }

    var wasError = false
    // check sources mapping
    for (i in modules.indices) {
        val m1 = modules[i]
        for (j in (i + 1) until modules.size) {
            val m2 = modules[j]
            val commonFiles = m1.sources.intersect(m2.sources)
            if (commonFiles.isNotEmpty()) {
                val message = buildString {
                    if (commonFiles.size == 1) {
                        append("File '${commonFiles.single()}'")
                    } else {
                        append("Files ")
                        append(commonFiles.joinToString(", ") { "'$it'" })
                    }
                    append(
                        " can be a part of only one module, but is listed as a source for both `${m1.name}` and `${m2.name}`, " +
                                "please check you $FRAGMENT_SOURCES_ARG_NAME options."
                    )
                }
                reportError(message)
                wasError = true
            }
        }
    }

    for (source in arguments.freeArgs) {
        if (source.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION)) continue
        if (modules.none { source in it.sources }) {
            reportError("Source '$source' does not belong to any module")
            wasError = true
        }
    }

    if (wasError) {
        return null
    }

    if (modules.size == 1) {
        if (rawFragmentRefines?.isNotEmpty() == true) {
            reportError("$FRAGMENT_REFINES_ARG_NAME flag is specified but there is only one module declared")
        }
        return HmppCliModuleStructure(modules, sourceDependencies = emptyMap(), moduleDependencies = emptyMap(), friendDependencies = emptyMap())
    }

    val duplicatedModules = modules.filter { module -> modules.count { it.name == module.name } > 1 }

    if (duplicatedModules.isNotEmpty()) {
        reportError("There are multiple modules with same name(s): ${duplicatedModules.distinct().joinToString(", ") { it.name }}")
        return null
    }

    val moduleByName = modules.associateBy { it.name }

    val sourceDependencies: Map<HmppCliModule, List<HmppCliModule>> = rawFragmentRefines.orEmpty().mapNotNull { rawFragmentRefinesEdge ->
        val split = rawFragmentRefinesEdge.split(":")
        if (split.size != 2) {
            reportError(
                "Incorrect syntax for $FRAGMENT_REFINES_ARG_NAME argument. " +
                        "Expected <fromModuleName>:<onModuleName> but got `$rawFragmentRefines`"
            )
            return@mapNotNull null
        }
        val moduleName1 = split[0]
        val moduleName2 = split[1]

        fun findModule(name: String): HmppCliModule? {
            return moduleByName[name].also { module ->
                if (module == null) {
                    reportError("`$FRAGMENT_REFINES_ARG_NAME=$rawFragmentRefinesEdge` Fragment `$name` not found in $FRAGMENTS_ARG_NAME arguments")
                }
            }
        }

        val module1 = findModule(moduleName1)
        val module2 = findModule(moduleName2)
        if (module1 == null || module2 == null) return@mapNotNull null
        module1 to module2
    }.groupBy(
        keySelector = { it.first },
        valueTransform = { it.second }
    )

    modules = DFS.topologicalOrder(modules) { sourceDependencies[it].orEmpty() }.asReversed()

    modules.forEachIndexed { i, module ->
        val dependencies = sourceDependencies[module].orEmpty()
        val previousModules = modules.subList(0, i)
        if (dependencies.any { it !in previousModules }) {
            reportError("There is a cycle in dependencies of module `${module.name}`")
        }
    }

    if (arguments.fragmentDependencies != null && !arguments.separateKmpCompilationScheme) {
        reportError("$FRAGMENT_DEPENDENCIES_ARG_NAME flag could be used only with ${CommonCompilerArguments::separateKmpCompilationScheme.cliArgument}")
    }
    if (arguments.fragmentFriendDependencies != null && !arguments.separateKmpCompilationScheme) {
        reportError("$FRAGMENT_FRIEND_DEPENDENCIES_ARG_NAME flag could be used only with ${CommonCompilerArguments::separateKmpCompilationScheme.cliArgument}")
    }

    fun buildFragmentDependencyMap(arguments: Array<String>?, argumentName: String): Map<HmppCliModule, MutableList<String>> {
        return buildMap {
            for (argument in arguments.orEmpty()) {
                val splitArg = argument.split(":", limit = 2)
                if (splitArg.size != 2) {
                    reportError(
                        "Incorrect syntax for $argumentName argument. " +
                                "Expected <moduleName>:<path> but got `$argument`"
                    )
                    continue
                }
                val (moduleName, dependency) = splitArg
                val module = moduleByName[moduleName] ?: run {
                    reportError("Module `$moduleName` not found in $FRAGMENTS_ARG_NAME arguments")
                    continue
                }
                val dependencies = getOrPut(module) { mutableListOf() }
                dependencies += dependency
            }
        }
    }

    val moduleDependencies = buildFragmentDependencyMap(arguments.fragmentDependencies, FRAGMENT_DEPENDENCIES_ARG_NAME)
    val friendDependencies = buildFragmentDependencyMap(arguments.fragmentFriendDependencies, FRAGMENT_FRIEND_DEPENDENCIES_ARG_NAME)

    return HmppCliModuleStructure(modules, sourceDependencies, moduleDependencies, friendDependencies)
}
