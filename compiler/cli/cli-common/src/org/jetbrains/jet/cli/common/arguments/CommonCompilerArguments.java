/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.arguments;

import com.intellij.util.SmartList;
import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.jetbrains.jet.cli.common.arguments.CommonArgumentConstants.SUPPRESS_WARNINGS;

public abstract class CommonCompilerArguments {
    @Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
    public boolean tags;

    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;

    @Argument(value = "version", description = "Display compiler version")
    public boolean version;

    @Argument(value = "help", alias = "h", description = "Print a synopsis of standard options")
    public boolean help;

    @Argument(value = "suppress", description = "Suppress all compiler warnings")
    @ValueDescription(SUPPRESS_WARNINGS)
    public String suppress;

    @Argument(value = "printArgs", description = "Print command line arguments")
    public boolean printArgs;

    public List<String> freeArgs = new SmartList<String>();

    public boolean suppressAllWarnings() {
        return SUPPRESS_WARNINGS.equalsIgnoreCase(suppress);
    }

    @NotNull
    public String executableScriptFileName() {
        return "kotlinc";
    }

    // Used only for serialize and deserialize settings. Don't use in other places!
    public static final class DummyImpl extends CommonCompilerArguments {}
}
