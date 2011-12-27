package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.compiler.AbstractCompileEnvironment;
import org.jetbrains.jet.compiler.CoreCompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {
    private KotlinCompiler() {
    }

    public static class Arguments {
        @Argument(value = "output", description = "output directory")
        public String outputDir;
        @Argument(value = "jar", description = "jar file name")
        public String jar;
        @Argument(value = "src", description = "source file or directory")
        public String src;
        @Argument(value = "module", description = "module to compile")
        public String module;
        @Argument(value = "includeRuntime", description = "include Kotlin runtime in to resulting jar")
        public boolean includeRuntime;
        @Argument(value = "stdlib", description = "means that we compile stdlib itself")
        public boolean stdlib;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (Throwable t) {
            System.out.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
            t.printStackTrace();
            return;
        }

        CoreCompileEnvironment environment = new CoreCompileEnvironment();

        try {
            environment.setJavaRuntime(AbstractCompileEnvironment.findRtJar(true));
            if (!environment.initializeKotlinRuntime()) {
                System.out.println("No runtime library found");
                return;
            }

            if (arguments.module != null) {
                environment.compileModuleScript(arguments.module, arguments.jar, arguments.includeRuntime);
                return;
            }
            else {
                environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime, arguments.stdlib);
            }
        } catch (CompileEnvironmentException e) {
            System.out.println(e.getMessage());
        }
    }

}
