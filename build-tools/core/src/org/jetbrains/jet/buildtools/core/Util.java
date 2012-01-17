package org.jetbrains.jet.buildtools.core;

import java.io.File;
import java.io.IOException;


/**
 * General convenient utilities.
 */
public final class Util {

    private Util () {
    }


    /**
     * {@code file.getCanonicalFile().getPath()} convenience wrapper.
     * @param f file to get its canonical path.
     * @return  file's canonical path
     */
    public static String getPath( File f ) {
        try {
            return f.getCanonicalFile().getPath();
        }
        catch ( IOException e ) {
            throw new RuntimeException( String.format( "Failed to resolve canonical file of [%s]: %s", f, e ), e );
        }
    }

}
