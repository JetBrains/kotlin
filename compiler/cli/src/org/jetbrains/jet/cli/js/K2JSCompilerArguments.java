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

package org.jetbrains.jet.cli.js;

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.CommonCompilerArguments;
import org.jetbrains.k2js.facade.MainCallParameters;


/**
 * NOTE: for now K2JSCompiler supports only minimal amount of parameters required to launch it from the plugin.
 */
public class K2JSCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", description = "Output file path")
    public String outputFile;

    @Argument(value = "libraryFiles", description = "Path to zipped lib sources or kotlin files")
    public String[] libraryFiles;

    @Argument(value = "sourceFiles", description = "Source files (dir or file)")
    public String[] sourceFiles;

    @Argument(value = "sourcemap", description = "Generate SourceMap")
    public boolean sourcemap;

    @Argument(value = "target", description = "Generate js files for specific ECMA version (now support only ECMA 5)")
    public String target;

    @Nullable
    @Argument(value = "main", description = "Whether a main function should be called; either 'call' or 'noCall', default 'call' (main function will be auto detected)")
    public String main;

    @Override
    public String getSrc() {
        throw new IllegalStateException();
    }

    public MainCallParameters createMainCallParameters() {
        if ("noCall".equals(main)) {
            return MainCallParameters.noCall();
        }
        else {
            // TODO should we pass the arguments to the compiler?
            return MainCallParameters.mainWithoutArguments();
        }
    }
}
