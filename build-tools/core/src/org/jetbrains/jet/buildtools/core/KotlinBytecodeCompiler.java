package org.jetbrains.jet.buildtools.core;

import org.jetbrains.jet.cli.KotlinCompiler;

import java.io.File;
import java.io.IOException;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class KotlinBytecodeCompiler {

    /**
     * Compiles a single file.
     *
     * @param sourceFile           source file to compile
     * @param destinationDirectory destination directory to compile the file to
     */
    public static void file ( File sourceFile, File destinationDirectory ) {

        String sourcePath      = getPath( sourceFile );
        String destinationPath = getPath( destinationDirectory );

        try {
            KotlinCompiler.main( "-src", sourcePath, "-output", destinationPath );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to compile [%s] to [%s]: %s", sourcePath, destinationPath, e ), e );
        }
    }


    /**
     * {@code file.getCanonicalFile().getPath()} convenience wrapper.
     * @param f file to get its canonical path.
     * @return  file's canonical path
     */
    private static String getPath( File f ) {
        try {
            return f.getCanonicalFile().getPath();
        }
        catch ( IOException e ) {
            throw new RuntimeException( String.format( "Failed to resolve canonical file of [%s]: %s", f, e ), e );
        }
    }
}
