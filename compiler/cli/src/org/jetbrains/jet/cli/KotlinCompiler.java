package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.compiler.CompileEnvironment;
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
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (Throwable t) {
            System.out.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] -src <filename or dirname>");
            t.printStackTrace();
            return;
        }

        CompileEnvironment environment = new CompileEnvironment();

        try {
            environment.setJavaRuntime(CompileEnvironment.findRtJar(true));
            if (!environment.initializeKotlinRuntime()) {
                System.out.println("No runtime library found");
                return;
            }

            if (arguments.module != null) {
                environment.compileModuleScript(arguments.module);
                return;
            }
            else {
                environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir);
            }
        } catch (CompileEnvironmentException e) {
            System.out.println(e.getMessage());
        }
    }

}
