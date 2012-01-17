package org.jetbrains.jet.buildtools.ant;

import static org.jetbrains.jet.buildtools.core.Util.*;
import org.apache.tools.ant.Task;
import org.jetbrains.jet.buildtools.core.KotlinBytecodeCompiler;

import java.io.File;


/**
 * Kotlin bytecode compiler Ant task.
 *
 * See
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 * http://evgeny-goldin.org/javadoc/ant/Tasks/javac.html - attribute names should be similar to {@code <javac>}.
 */
public class KotlinBytecodeCompilerTask extends Task {

    private File srcdir;
    private File file;
    private File destdir;

    public void setSrcdir  ( File srcdir  ) { this.srcdir  = srcdir;  }
    public void setFile    ( File file    ) { this.file    = file;    }
    public void setDestdir ( File destdir ) { this.destdir = destdir; }


    @Override
    public void execute() {

        if ( this.destdir == null ) {
            throw new RuntimeException( "\"destdir\" attribute is not specified" );
        }

        if (( this.srcdir != null ) || ( this.file != null )) {
            String src  = getPath( this.srcdir != null ? this.srcdir : this.file );
            String dest = getPath( this.destdir );

            log( String.format( "[%s] => [%s]", src, dest ));
            KotlinBytecodeCompiler.compileSources( src, dest );
        }
        else {
            throw new RuntimeException( String.format( "Operation is not supported" ));
        }
    }
}
