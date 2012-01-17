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

        CompileEnvironment environment = new CompileEnvironment();
        environment.setJavaRuntime( CompileEnvironment.findRtJar( true ));

        if ( ! environment.initializeKotlinRuntime()) {
            throw new RuntimeException( "No Kotlin runtime library found" );
        }

        return environment;
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
     * @param source      compilation source (directory or file)
     * @param destination compilation destination
     */
    public void sourcesToDir ( String source, String destination, boolean includeRuntime, boolean excludeStdlib ) {
        boolean success = ENV.compileBunchOfSources( source, null, destination, includeRuntime, ! excludeStdlib );
        if ( ! success ) {
            throw new RuntimeException( compilationError( source ));
        }
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param source compilation source (directory or file)
     * @param jar    compilation destination jar
     */
    public void sourcesToJar ( String source, String jar, boolean includeRuntime, boolean excludeStdlib ) {
        boolean success = ENV.compileBunchOfSources( source, jar, null, includeRuntime, ! excludeStdlib );
        if ( ! success ) {
            throw new RuntimeException( compilationError( source ));
        }
    }
}
