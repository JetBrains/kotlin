/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
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

    @GradleOption(DefaultValues.LanguageVersions::class)
    @Argument(
            value = "-language-version",
            valueDescription = "<version>",
            description = "Provide source compatibility with specified language version"
    )
    var languageVersion: String? by FreezableVar(null)

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

    // TODO Remove in 1.0
    @Argument(
            value = "-Xrepeat",
            valueDescription = "<count>",
            description = "Repeat compilation (for performance analysis)"
    )
    var repeat: String? by FreezableVar(null)

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

    open fun configureAnalysisFlags(collector: MessageCollector): MutableMap<AnalysisFlag<*>, Any> {
        return HashMap<AnalysisFlag<*>, Any>().apply {
            put(AnalysisFlag.skipMetadataVersionCheck, skipMetadataVersionCheck)
            put(AnalysisFlag.multiPlatformDoNotCheckActual, noCheckActual)
        }
    }

    // Used only for serialize and deserialize settings. Don't use in other places!
    class DummyImpl : CommonCompilerArguments()
}
