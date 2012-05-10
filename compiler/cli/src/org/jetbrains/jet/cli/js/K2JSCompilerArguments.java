/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.js;

import com.sampullara.cli.Argument;
import org.jetbrains.jet.cli.common.CompilerArguments;

/**
 * @author Pavel Talanov
 */

/**
 * NOTE: for now K2JSCompiler supports only minimal amount of parameters required to launch it from the plugin.
 * You can specify only one source folder, path to the file where generated file will be stored, path to zipped library sources.
 */
public class K2JSCompilerArguments extends CompilerArguments {
    @Argument(value = "output", description = "Output file path")
    public String outputFile;

    //NOTE: may well be a subject to change soon
    @Argument(value = "libzip", description = "Path to zipped lib sources")
    public String libzip;

    @Argument(value = "srcdir", description = "Sources directory")
    public String srcdir;

    @Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
    public boolean tags;

    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;

    @Argument(value = "version", description = "Display compiler version")
    public boolean version;

    @Argument(value = "help", alias = "h", description = "Show help")
    public boolean help;

    @Override
    public boolean isHelp() {
        return help;
    }

    @Override
    public boolean isTags() {
        return tags;
    }

    @Override
    public boolean isVersion() {
        return version;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }
}
