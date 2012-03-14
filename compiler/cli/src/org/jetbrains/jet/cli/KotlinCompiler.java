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

package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;
import org.jetbrains.jet.compiler.MessageRenderer;

import java.io.PrintStream;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {

    public static void main(String... args) {
        doMain(new KotlinCompiler(), args);
    }

    /**
     * Useful main for derived command line tools
     */
    public static void doMain(KotlinCompiler compiler, String[] args) {
        try {
            int rc = compiler.exec(args);
            if (rc != 0) {
                System.err.println("exec() finished with " + rc + " return code");
                System.exit(rc);
            }
        } catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public int exec(String... args) {
        return exec(System.out, args);
    }

    public int exec(PrintStream errStream, String... args) {
        CompilerArguments arguments = createArguments();
        if (!parseArguments(errStream, arguments, args)) {
            return 1;
        }
        return exec(errStream, arguments);
    }

    /**
     * Executes the compiler on the parsed arguments
     */
    public int exec(PrintStream errStream, CompilerArguments arguments) {
        if (arguments.help) {
            usage(errStream);
            return 0;
        }
        System.setProperty("java.awt.headless", "true");

        MessageRenderer messageRenderer = arguments.tags ? MessageRenderer.TAGS : MessageRenderer.PLAIN;
        CompileEnvironment environment = new CompileEnvironment(messageRenderer, arguments.verbose);
        try {
            configureEnvironment(environment, arguments, errStream);

            boolean noErrors;
            if (arguments.module != null) {
                noErrors = environment.compileModuleScript(arguments.module, arguments.jar, arguments.outputDir, arguments.includeRuntime);
            }
            else {
                noErrors = environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime);
            }
            return noErrors ? 0 : 1;
        }
        catch (Throwable t) {
            errStream.println(messageRenderer.renderException(t));
            return 1;
        }
        finally {
            environment.dispose();
        }
    }

    /**
     * Returns true if the arguments can be parsed correctly
     */
    protected boolean parseArguments(PrintStream errStream, CompilerArguments arguments, String[] args) {
        try {
            Args.parse(arguments, args);
            return true;
        }
        catch (IllegalArgumentException e) {
            usage(System.err);
        }
        catch (Throwable t) {
            // Always use tags
            errStream.println(MessageRenderer.TAGS.renderException(t));
        }
        return false;
    }

    protected void usage(PrintStream target) {
        target.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected CompilerArguments createArguments() {
        return new CompilerArguments();
    }

    /**
     * Strategy method to configure the environment, allowing compiler
     * based tools to customise their own plugins
     */
    protected void configureEnvironment(CompileEnvironment environment, CompilerArguments arguments, PrintStream errStream) {
        environment.setIgnoreErrors(false);
        environment.setErrorStream(errStream);

        environment.setStubs(arguments.stubs);

        if (arguments.docOutputDir != null) {
            KDocLoader.install(arguments.docOutputDir, environment.getMyEnvironment());
        }

        if (arguments.stdlib != null) {
            environment.setStdlib(arguments.stdlib);
        }

        if (arguments.classpath != null) {
            environment.addToClasspath(arguments.classpath);
        }
    }
}
