package org.jetbrains.jet.buildtools.core;

import org.jetbrains.jet.compiler.CompileEnvironment;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class BytecodeCompiler {

    private final CompileEnvironment ENV = createCompileEnvironment();


    public BytecodeCompiler () {
    }


    /**
     * Creates and initializes new {@link CompileEnvironment} instance.
     * @return new {@link CompileEnvironment} instance
     */
    private CompileEnvironment createCompileEnvironment () {
        return new CompileEnvironment();
    }


    /**
     * Retrieves compilation error message.
     * @param  source compilation source
     * @return compilation error message
     */
    private String compilationError ( String source ) {
        return String.format( "[%s] compilation failed, see \"ERROR:\" messages above for more details.", source );
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param source        compilation source (directory or file)
     * @param destination   compilation destination
     * @param excludeStdlib whether Kotlin standard library is excluded in compilation
     */
    public void sourcesToDir ( String source, String destination, boolean excludeStdlib ) {
        boolean success = ENV.compileBunchOfSources( source, null, destination, true);
        if ( ! success ) {
            throw new RuntimeException( compilationError( source ));
        }
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param source         compilation source (directory or file)
     * @param jar            compilation destination jar
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     * @param excludeStdlib whether Kotlin standard library is excluded in compilation
     */
    public void sourcesToJar ( String source, String jar, boolean includeRuntime, boolean excludeStdlib ) {
        boolean success = ENV.compileBunchOfSources( source, jar, null, includeRuntime);
        if ( ! success ) {
            throw new RuntimeException( compilationError( source ));
        }
    }


    /**
     * {@code CompileEnvironment#compileModuleScript} wrapper.
     *
     * @param moduleFile     compilation module file
     * @param jar            compilation destination jar
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     */
    public void moduleToJar ( String moduleFile, String jar, boolean includeRuntime ) {
        ENV.compileModuleScript( moduleFile, jar, includeRuntime );
    }
}
