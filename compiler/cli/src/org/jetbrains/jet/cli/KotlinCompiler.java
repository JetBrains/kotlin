package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;
import org.jetbrains.jet.compiler.FileNameTransformer;

import java.io.PrintStream;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {
    private static final FileNameTransformer ANY_EXTENSION_TO_JAVA = new FileNameTransformer() {
        @NotNull
        @Override
        public String transformFileName(@NotNull String fileName) {
            return fileName.replaceFirst("\\.\\w+$", ".java");
        }
    };

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

        @Argument(value = "stdlib", description = "Path to the stdlib.jar")
        public String stdlib;

        @Argument(value = "help", alias = "h", description = "show help")
        public boolean help;

        @Argument(value = "ignoreErrors", description = "Emit byte code even if there are compilation errors (not recommended)")
        public boolean ignoreErrors;

        @Argument(value = "transformNamesToJava", description = "Transform Kotlin file names to *.java. This option is needed for compiling kotlinized Java library headers")
        public boolean transformNamesToJava;
    }

    private static void usage(PrintStream target) {
        target.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
    }

    public static void main(String... args) {
        try {
            int rc = exec(args);
            if (rc != 0) {
                System.err.println("exec() finished with " + rc + " return code");
                System.exit(rc);
            }
        } catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static int exec(String... args) {
        return exec(System.out, args);
    }

    public static int exec(PrintStream errStream, String... args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (IllegalArgumentException e) {
            usage(System.err);
            return 1;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return 1;
        }

        if (arguments.help) {
            usage(errStream);
            return 0;
        }

        CompileEnvironment environment = new CompileEnvironment(arguments.transformNamesToJava ? ANY_EXTENSION_TO_JAVA : FileNameTransformer.IDENTITY);
        try {
            environment.setIgnoreErrors(arguments.ignoreErrors);
            environment.setErrorStream(errStream);

            if (arguments.stdlib != null) {
                environment.setStdlib(arguments.stdlib);
            }

            if (arguments.module != null) {
                environment.compileModuleScript(arguments.module, arguments.jar, arguments.outputDir, arguments.includeRuntime);
                return 0;
            }
            else {
                if (!environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime)) {
                    return 1;
                }
            }

            return 0;
        } finally {
            environment.dispose();
        }
    }
}
