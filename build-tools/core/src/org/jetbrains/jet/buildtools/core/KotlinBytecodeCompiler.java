package org.jetbrains.jet.buildtools.core;

import org.jetbrains.jet.compiler.CompileEnvironment;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class KotlinBytecodeCompiler {


    private static CompileEnvironment environment() {
        CompileEnvironment environment = new CompileEnvironment();
        environment.setJavaRuntime(CompileEnvironment.findRtJar( true ));

        if ( ! environment.initializeKotlinRuntime()) {
            throw new RuntimeException( "No Kotlin runtime library found" );
        }

        return environment;
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param source      compilation source
     * @param destination compilation destination
     */
    public static void compileSources ( String source, String destination ) {
        boolean success = environment().compileBunchOfSources( source, null, destination, true, false );
        if ( ! success ) {
            throw new RuntimeException( String.format( "[%s] compilation failed, see \"ERROR:\" messages above for more details.", source ));
        }
    }
}
