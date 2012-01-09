package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;

import java.io.PrintStream;

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
        @Argument(value = "excludeStdlib", description = "do not load stdlib (to compile stdlib itself)")
        public boolean excludeStdlib;
        @Argument(value = "help", alias = "h", description = "show help")
        public boolean help;
    }
    
    private static void usage(PrintStream target) {
        target.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] [-src <filename or dirname>|-module <module file>] [-includeRuntime] [-excludeStdlib]");
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (IllegalArgumentException e) {
            usage(System.err);
            System.exit(1);
            return;
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
            return;
        }
        
        if (arguments.help) {
            usage(System.out);
            System.exit(0);
        }

        CompileEnvironment environment = new CompileEnvironment();

        try {
            environment.setJavaRuntime(CompileEnvironment.findRtJar(true));
            if (!environment.initializeKotlinRuntime()) {
                System.err.println("No Kotlin runtime library found");
                System.exit(1);
                return;
            }

            if (arguments.module != null) {
                environment.compileModuleScript(arguments.module, arguments.jar, arguments.includeRuntime);
                return;
            }
            else {
                if (!environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime, !arguments.excludeStdlib)) {
                    System.exit(1);
                    return;
                }
            }
        } catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
    }

}
