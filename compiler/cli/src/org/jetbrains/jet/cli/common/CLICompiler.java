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

package org.jetbrains.jet.cli.common;

import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.MessageRenderer;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException;

import java.io.PrintStream;
import java.util.List;

import static org.jetbrains.jet.cli.common.ExitCode.INTERNAL_ERROR;
import static org.jetbrains.jet.cli.common.ExitCode.OK;

/**
 * @author Pavel Talanov
 */
public abstract class CLICompiler<CLArgs extends CompilerArguments, CEConf extends CompileEnvironmentConfig> {

    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull String... args) {
        CLArgs arguments = createArguments();
        if (!parseArguments(errStream, arguments, args)) {
            return INTERNAL_ERROR;
        }
        return exec(errStream, arguments);
    }

    /**
     * Returns true if the arguments can be parsed correctly
     */
    protected boolean parseArguments(@NotNull PrintStream errStream, @NotNull CLArgs arguments, @NotNull String[] args) {
        try {
            Args.parse(arguments, args);
            return true;
        }
        catch (IllegalArgumentException e) {
            usage(errStream);
        }
        catch (Throwable t) {
            // Always use tags
            errStream.println(MessageRenderer.TAGS.renderException(t));
        }
        return false;
    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected void usage(@NotNull PrintStream target) {
        ArgsUtil.printUsage(target, createArguments());
    }

    /**
     * Strategy method to configure the environment, allowing compiler
     * based tools to customise their own plugins
     */
    protected void configureEnvironment(@NotNull CEConf configuration, @NotNull CLArgs arguments) {
        List<CompilerPlugin> plugins = arguments.getCompilerPlugins();
        configuration.getCompilerPlugins().addAll(plugins);
    }

    @NotNull
    protected abstract CLArgs createArguments();

    @NotNull
    public abstract ExitCode exec(PrintStream errStream, CLArgs arguments);

    /**
     * Useful main for derived command line tools
     */
    public static void doMain(@NotNull CLICompiler compiler, @NotNull String[] args) {
        try {
            ExitCode rc = compiler.exec(System.out, args);
            if (rc != OK) {
                System.err.println("exec() finished with " + rc + " return code");
                System.exit(rc.getCode());
            }
        }
        catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(INTERNAL_ERROR.getCode());
        }
    }
}
