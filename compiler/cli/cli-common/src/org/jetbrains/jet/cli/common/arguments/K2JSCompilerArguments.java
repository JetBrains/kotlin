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

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.jet.cli.common.arguments.K2JsArgumentConstants.CALL;
import static org.jetbrains.jet.cli.common.arguments.K2JsArgumentConstants.NO_CALL;

/**
 * NOTE: for now K2JSCompiler supports only minimal amount of parameters required to launch it from the plugin.
 */
public class K2JSCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", description = "Output file path")
    @ValueDescription("<path>")
    public String outputFile;

    @Argument(value = "libraryFiles", description = "Path to zipped library sources or kotlin files separated by commas")
    @ValueDescription("<path[,]>")
    public String[] libraryFiles;

    @Argument(value = "sourcemap", description = "Generate SourceMap")
    public boolean sourcemap;

    @Argument(value = "target", description = "Generate JS files for specific ECMA version (only ECMA 5 is supported)")
    @ValueDescription("<version>")
    public String target;

    @Nullable
    @Argument(value = "main", description = "Whether a main function should be called; default '" + CALL + "' (main function will be auto detected)")
    @ValueDescription("{" + CALL + "," + NO_CALL + "}")
    public String main;

    @Argument(value = "outputPrefix", description = "Path to file which will be added to the beginning of output file")
    @ValueDescription("<path>")
    public String outputPrefix;

    @Argument(value = "outputPostfix", description = "Path to file which will be added to the end of output file")
    @ValueDescription("<path>")
    public String outputPostfix;

    @Override
    @NotNull
    public String executableScriptFileName() {
        return "kotlinc-js";
    }
}
