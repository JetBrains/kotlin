package org.jetbrains.jet.buildtools.ant;

import static org.jetbrains.jet.buildtools.core.Util.*;
import org.apache.tools.ant.Task;
import org.jetbrains.jet.buildtools.core.BytecodeCompiler;

import java.io.File;


/**
 * Kotlin bytecode compiler Ant task.
 *
 * See
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 * http://evgeny-goldin.org/javadoc/ant/Tasks/javac.html - attribute names should be similar to {@code <javac>}.
 */
public class BytecodeCompilerTask extends Task {

    private final BytecodeCompiler compiler = new BytecodeCompiler();

    private File    module;
    private File    srcdir;
    private File    file;
    private File    destdir;
    private File    destjar;
    private boolean includeRuntime = true;
    private boolean excludeStdlib  = false;

    public void setModule  ( File module  ) { this.module  = module;  }
    public void setSrcdir  ( File srcdir  ) { this.srcdir  = srcdir;  }
    public void setFile    ( File file    ) { this.file    = file;    }
    public void setDestdir ( File destdir ) { this.destdir = destdir; }
    public void setDestjar ( File destjar ) { this.destjar = destjar; }

    public void setIncludeRuntime ( boolean includeRuntime ) { this.includeRuntime = includeRuntime; }
    public void setExcludeStdlib  ( boolean excludeStdlib  ) { this.excludeStdlib  = excludeStdlib;  }


    @Override
    public void execute() {

        if (( this.srcdir != null ) || ( this.file != null )) {

            if (( this.destdir == null ) && ( this.destjar == null )) {
                throw new RuntimeException( "\"destdir\" or \"destjar\" should be specified" );
            }

            String src  = getPath( this.srcdir  != null ? this.srcdir  : this.file    );
            String dest = getPath( this.destdir != null ? this.destdir : this.destjar );

            log( String.format( "[%s] => [%s]", src, dest ));

            if ( this.destdir != null ) {
                compiler.sourcesToDir( src, dest, this.excludeStdlib );
            }
            else {
                compiler.sourcesToJar( src, dest, this.includeRuntime, this.excludeStdlib );
            }
        }
        else if ( this.module != null ) {

            if ( this.destdir != null ) {
                throw new RuntimeException( "Module compilation is only supported for jar destination" );
            }

            String modulePath = getPath( this.module );
            String jarPath    = ( this.destjar != null ? getPath( this.destjar ) : null );

            compiler.moduleToJar( modulePath, jarPath, this.includeRuntime );
        }
        else {
            throw new RuntimeException( "This operation is not supported" );
        }
    }
}
