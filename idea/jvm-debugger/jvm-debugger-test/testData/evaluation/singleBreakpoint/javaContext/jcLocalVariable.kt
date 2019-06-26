// FILE: jcLocalVariable.kt
package jcLocalVariable

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.localVariable()
}

// STEP_INTO: 1
// STEP_OVER: 1

// EXPRESSION: i
// RESULT: 1: I

// FILE: forTests/javaContext/JavaClass.java
package forTests.javaContext;

public class JavaClass {
    public void localVariable() {
        int i = 1;
        int breakpoint = 1;
    }
}