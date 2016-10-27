/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.util.SmartList;
import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CommonCompilerArguments {
    public static final String PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>";

    @GradleOption(DefaultValues.LanguageVersions.class)
    @Argument(value = "language-version", description = "Provide source compatibility with specified language version")
    @ValueDescription("<version>")
    public String languageVersion;

    @GradleOption(DefaultValues.LanguageVersions.class)
    @Argument(value = "api-version", description = "Allow to use declarations only from the specified version of bundled libraries")
    @ValueDescription("<version>")
    public String apiVersion;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "nowarn", description = "Generate no warnings")
    public boolean suppressWarnings;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;

    @Argument(value = "version", description = "Display compiler version")
    public boolean version;

    @Argument(value = "help", alias = "h", description = "Print a synopsis of standard options")
    public boolean help;

    @Argument(value = "X", description = "Print a synopsis of advanced options")
    public boolean extraHelp;

    @Argument(value = "Xno-inline", description = "Disable method inlining")
    public boolean noInline;

    // TODO Remove in 1.0
    @Argument(value = "Xrepeat", description = "Repeat compilation (for performance analysis)")
    @ValueDescription("<count>")
    public String repeat;

    @Argument(value = "Xplugin", description = "Load plugins from the given classpath")
    @ValueDescription("<path>")
    public String[] pluginClasspaths;

    @Argument(value = "P", description = "Pass an option to a plugin")
    @ValueDescription(PLUGIN_OPTION_FORMAT)
    public String[] pluginOptions;

    public List<String> freeArgs = new SmartList<String>();

    public List<String> unknownExtraFlags = new SmartList<String>();

    @NotNull
    public String executableScriptFileName() {
        return "kotlinc";
    }

    // Used only for serialize and deserialize settings. Don't use in other places!
    public static final class DummyImpl extends CommonCompilerArguments {}
}
