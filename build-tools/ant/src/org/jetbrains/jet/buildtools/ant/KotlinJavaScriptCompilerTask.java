package org.jetbrains.jet.buildtools.ant;

import org.apache.tools.ant.Task;


/**
 * Kotlin JavaScript compiler Ant task.
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 */
public class KotlinJavaScriptCompilerTask extends Task {

    @Override
    public void execute() {
        log( "KotlinJavaScriptCompilerTask" );
    }
}
