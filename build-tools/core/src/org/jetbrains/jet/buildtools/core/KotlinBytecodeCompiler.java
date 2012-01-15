package org.jetbrains.jet.buildtools.core;

import org.jetbrains.jet.cli.KotlinCompiler;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class KotlinBytecodeCompiler {

    /**
     * Invokes Kotlin compiler with "-src" and "-output" options.
     * @param source      "-src" option to specify
     * @param destination "-output" option to specify
     */
    public static void src ( String source, String destination ) {
        KotlinCompiler.main( "-src", source, "-output", destination );
    }

}
