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

    private File    output;
    private File    jar;
    private File    stdlib;
    private File    src;
    private File    module;
    private boolean includeRuntime = true;

    public void setOutput         ( File output            ) { this.output         = output;         }
    public void setJar            ( File jar               ) { this.jar            = jar;            }
    public void setStdlib         ( File stdlib            ) { this.stdlib         = stdlib;         }
    public void setSrc            ( File src               ) { this.src            = src;            }
    public void setModule         ( File module            ) { this.module         = module;         }
    public void setIncludeRuntime ( boolean includeRuntime ) { this.includeRuntime = includeRuntime; }


    @Override
    public void execute() {

        final BytecodeCompiler compiler = new BytecodeCompiler( this.stdlib != null ? getPath( this.stdlib ) : null );

        if ( this.src != null ) {

            if (( this.output == null ) && ( this.jar == null )) {
                throw new RuntimeException( "\"output\" or \"jar\" should be specified" );
            }

            String source      = getPath( this.src );
            String destination = getPath( this.output != null ? this.output : this.jar );

            log( String.format( "[%s] => [%s]", source, destination ));

            if ( this.output != null ) {
                compiler.sourcesToDir( source, destination );
            }
            else {
                compiler.sourcesToJar( source, destination, this.includeRuntime );
            }
        }
        else if ( this.module != null ) {

            if ( this.output != null ) {
                throw new RuntimeException( "Module compilation is only supported for jar destination" );
            }

            String modulePath = getPath( this.module );
            String jarPath    = ( this.jar != null ? getPath( this.jar ) : null );

            compiler.moduleToJar( modulePath, jarPath, this.includeRuntime );
        }
        else {
            throw new RuntimeException( "\"src\" or \"module\" should be specified" );
        }
    }
}
