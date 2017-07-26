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

package org.jetbrains.kotlin.cli.common.arguments;

import org.jetbrains.kotlin.utils.SmartList;

import java.io.Serializable;
import java.util.List;

public abstract class CommonToolArguments implements Serializable {
    private static final long serialVersionUID = 0L;

    public List<String> freeArgs = new SmartList<>();

    public transient ArgumentParseErrors errors = new ArgumentParseErrors();

    @Argument(value = "-help", shortName = "-h", description = "Print a synopsis of standard options")
    public boolean help;

    @Argument(value = "-X", description = "Print a synopsis of advanced options")
    public boolean extraHelp;

    @Argument(value = "-version", description = "Display compiler version")
    public boolean version;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "-verbose", description = "Enable verbose logging output")
    public boolean verbose;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "-nowarn", description = "Generate no warnings")
    public boolean suppressWarnings;
}
