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

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import java.util.*

@SuppressWarnings("WeakerAccess")
abstract class CommonCompilerArguments : CommonToolArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L

        const val PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>"

        const val WARN = "warn"
        const val ERROR = "error"
        const val ENABLE = "enable"
    }

    @get:Transient
    var autoAdvanceLanguageVersion: Boolean by FreezableVar(true)

    @GradleOption(DefaultValues.LanguageVersions::class)
    @Argument(
            value = "-language-version",
            valueDescription = "<version>",
            description = "Provide source compatibility with specified language version"
    )
    var languageVersion: String? by FreezableVar(null)

    @get:Transient
    var autoAdvanceApiVersion: Boolean by FreezableVar(true)

    @GradleOption(DefaultValues.LanguageVersions::class)
    @Argument(
            value = "-api-version",
            valueDescription = "<version>",
            description = "Allow to use declarations only from the specified version of bundled libraries"
    )
    var apiVersion: String? by FreezableVar(null)

    @Argument(
            value = "-kotlin-home",
            valueDescription = "<path>",
            description = "Path to Kotlin compiler home directory, used for runtime libraries discovery"
    )
    var kotlinHome: String? by FreezableVar(null)

    @Argument(value = "-P", valueDescription = PLUGIN_OPTION_FORMAT, description = "Pass an option to a plugin")
    var pluginOptions: Array<String>? by FreezableVar(null)

    // Advanced options

    @Argument(value = "-Xno-inline", description = "Disable method inlining")
    var noInline: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xskip-metadata-version-check",
            description = "Load classes with bad metadata version anyway (incl. pre-release classes)"
    )
    var skipMetadataVersionCheck: Boolean by FreezableVar(false)

    @Argument(value = "-Xallow-kotlin-package", description = "Allow compiling code in package 'kotlin' and allow not requiring kotlin.stdlib in module-info")
    var allowKotlinPackage: Boolean by FreezableVar(false)

    @Argument(value = "-Xreport-output-files", description = "Report source to output files mapping")
    var reportOutputFiles: Boolean by FreezableVar(false)

    @Argument(value = "-Xplugin", valueDescription = "<path>", description = "Load plugins from the given classpath")
    var pluginClasspaths: Array<String>? by FreezableVar(null)

    @Argument(value = "-Xmulti-platform", description = "Enable experimental language support for multi-platform projects")
    var multiPlatform: Boolean by FreezableVar(false)

    @Argument(value = "-Xno-check-actual", description = "Do not check presence of 'actual' modifier in multi-platform projects")
    var noCheckActual: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xintellij-plugin-root",
            valueDescription = "<path>",
            description = "Path to the kotlin-compiler.jar or directory where IntelliJ configuration files can be found"
    )
    var intellijPluginRoot: String? by FreezableVar(null)

    @Argument(
            value = "-Xcoroutines",
            valueDescription = "{enable|warn|error}",
            description = "Enable coroutines or report warnings or errors on declarations and use sites of 'suspend' modifier"
    )
    var coroutinesState: String? by FreezableVar(WARN)

    @Argument(
            value = "-Xnew-inference",
            description = "Enable new experimental generic type inference algorithm"
    )
    var newInference: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xlegacy-smart-cast-after-try",
            description = "Allow var smart casts despite assignment in try block"
    )
    var legacySmartCastAfterTry: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xeffect-system",
            description = "Enable experimental language feature: effect system"
    )
    var effectSystem: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xread-deserialized-contracts",
        description = "Enable reading of contracts from metadata"
    )
    var readDeserializedContracts: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xexperimental",
            valueDescription = "<fq.name>",
            description = "Enable and propagate usages of experimental API for marker annotation with the given fully qualified name"
    )
    var experimental: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xuse-experimental",
        valueDescription = "<fq.name>",
        description = "Enable usages of COMPILATION-affecting experimental API for marker annotation with the given fully qualified name"
    )
    var useExperimental: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xproper-ieee754-comparisons",
        description = "Generate proper IEEE 754 comparisons in all cases if values are statically known to be of primitive numeric types"
    )
    var properIeee754Comparisons by FreezableVar(false)

    @Argument(value = "-Xreport-perf", description = "Report detailed performance statistics")
    var reportPerf: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xdump-perf",
        valueDescription = "<path>",
        description = "Dump detailed performance statistics to the specified file"
    )
    var dumpPerf: String? by FreezableVar(null)

    open fun configureAnalysisFlags(collector: MessageCollector): MutableMap<AnalysisFlag<*>, Any> {
        return HashMap<AnalysisFlag<*>, Any>().apply {
            put(AnalysisFlag.skipMetadataVersionCheck, skipMetadataVersionCheck)
            put(AnalysisFlag.multiPlatformDoNotCheckActual, noCheckActual)
            put(AnalysisFlag.allowKotlinPackage, allowKotlinPackage)
            put(AnalysisFlag.experimental, experimental?.toList().orEmpty())
            put(AnalysisFlag.useExperimental, useExperimental?.toList().orEmpty())
            put(AnalysisFlag.explicitApiVersion, apiVersion != null)
        }
    }

    open fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> =
        HashMap<LanguageFeature, LanguageFeature.State>().apply {
            if (multiPlatform) {
                put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
            }

            when (coroutinesState) {
                CommonCompilerArguments.ERROR -> put(LanguageFeature.Coroutines, LanguageFeature.State.ENABLED_WITH_ERROR)
                CommonCompilerArguments.ENABLE -> put(LanguageFeature.Coroutines, LanguageFeature.State.ENABLED)
                CommonCompilerArguments.WARN -> {}
                else -> {
                    val message = "Invalid value of -Xcoroutines (should be: enable, warn or error): " + coroutinesState
                    collector.report(CompilerMessageSeverity.ERROR, message, null)
                }
            }

            if (newInference) {
                put(LanguageFeature.NewInference, LanguageFeature.State.ENABLED)
            }

            if (legacySmartCastAfterTry) {
                put(LanguageFeature.SoundSmartCastsAfterTry, LanguageFeature.State.DISABLED)
            }

            if (effectSystem) {
                put(LanguageFeature.UseCallsInPlaceEffect, LanguageFeature.State.ENABLED)
                put(LanguageFeature.UseReturnsEffect, LanguageFeature.State.ENABLED)
            }

            if (readDeserializedContracts) {
                put(LanguageFeature.ReadDeserializedContracts, LanguageFeature.State.ENABLED)
            }

            if (properIeee754Comparisons) {
                put(LanguageFeature.ProperIeee754Comparisons, LanguageFeature.State.ENABLED)
            }
        }

    fun configureLanguageVersionSettings(collector: MessageCollector): LanguageVersionSettings {

        // If only "-api-version" is specified, language version is assumed to be the latest stable
        val languageVersion = parseVersion(collector, languageVersion, "language") ?: LanguageVersion.LATEST_STABLE

        // If only "-language-version" is specified, API version is assumed to be equal to the language version
        // (API version cannot be greater than the language version)
        val apiVersion = parseVersion(collector, apiVersion, "API") ?: languageVersion

        if (apiVersion > languageVersion) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})"
            )
        }

        if (!languageVersion.isStable) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for new language and library features"
            )
        }

        return LanguageVersionSettingsImpl(
            languageVersion,
            ApiVersion.createByLanguageVersion(apiVersion),
            configureAnalysisFlags(collector),
            configureLanguageFeatures(collector)
        )
    }

    private fun parseVersion(collector: MessageCollector, value: String?, versionOf: String): LanguageVersion? =
        if (value == null) null
        else LanguageVersion.fromVersionString(value)
                ?: run {
                    val versionStrings = LanguageVersion.values().map(LanguageVersion::description)
                    val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
                    collector.report(CompilerMessageSeverity.ERROR, message, null)
                    null
                }

    // Used only for serialize and deserialize settings. Don't use in other places!
    class DummyImpl : CommonCompilerArguments()
}
