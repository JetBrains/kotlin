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

package org.jetbrains.kotlin.cli.common.arguments;

import org.jetbrains.annotations.NotNull;

public abstract class CommonCompilerArguments extends CommonToolArguments {
    public static final long serialVersionUID = 0L;

    public static final String PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>";

    @GradleOption(DefaultValues.LanguageVersions.class)
    @Argument(
            value = "-language-version",
            valueDescription = "<version>",
            description = "Provide source compatibility with specified language version"
    )
    public String languageVersion;

    @GradleOption(DefaultValues.LanguageVersions.class)
    @Argument(
            value = "-api-version",
            valueDescription = "<version>",
            description = "Allow to use declarations only from the specified version of bundled libraries"
    )
    public String apiVersion;

    @Argument(value = "-P", valueDescription = PLUGIN_OPTION_FORMAT, description = "Pass an option to a plugin")
    public String[] pluginOptions;

    // Advanced options

    @Argument(value = "-Xno-inline", description = "Disable method inlining")
    public boolean noInline;

    // TODO Remove in 1.0
    @Argument(
            value = "-Xrepeat",
            valueDescription = "<count>",
            description = "Repeat compilation (for performance analysis)"
    )
    public String repeat;

    @Argument(value = "-Xskip-metadata-version-check", description = "Load classes with bad metadata version anyway (incl. pre-release classes)")
    public boolean skipMetadataVersionCheck;

    @Argument(value = "-Xallow-kotlin-package", description = "Allow compiling code in package 'kotlin'")
    public boolean allowKotlinPackage;

    @Argument(value = "-Xreport-output-files", description = "Report source to output files mapping")
    public boolean reportOutputFiles;

    @Argument(value = "-Xplugin", valueDescription = "<path>", description = "Load plugins from the given classpath")
    public String[] pluginClasspaths;

    @Argument(value = "-Xmulti-platform", description = "Enable experimental language support for multi-platform projects")
    public boolean multiPlatform;

    @Argument(value = "-Xno-check-impl", description = "Do not check presence of 'impl' modifier in multi-platform projects")
    public boolean noCheckImpl;

    @Argument(
            value = "-Xintellij-plugin-root",
            valueDescription = "<path>",
            description = "Path to the kotlin-compiler.jar or directory where IntelliJ configuration files can be found"
    )
    public String intellijPluginRoot;

    @Argument(
            value = "-Xcoroutines",
            valueDescription = "{enable|warn|error}",
            description = "Enable coroutines or report warnings or errors on declarations and use sites of 'suspend' modifier"
    )
    public String coroutinesState = WARN;

    @NotNull
    public static CommonCompilerArguments createDefaultInstance() {
        return new DummyImpl();
    }

    public static final String WARN = "warn";
    public static final String ERROR = "error";
    public static final String ENABLE = "enable";

    // Used only for serialize and deserialize settings. Don't use in other places!
    public static final class DummyImpl extends CommonCompilerArguments {}
}
