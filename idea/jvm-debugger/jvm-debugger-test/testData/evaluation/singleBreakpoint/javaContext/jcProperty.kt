// FILE: test.kt
package jcProperty

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.property()
}

// STEP_INTO: 1

// EXPRESSION: this.javaProperty
// RESULT: 1: I

// EXPRESSION: javaProperty
// RESULT: 1: I

// EXPRESSION: javaPrivateProperty
// RESULT: 1: I

// FILE: forTests/javaContext/JavaClass.java
package forTests.javaContext;

public class JavaClass {
    public int javaProperty = 1;
    private int javaPrivateProperty = 1;

    public void property() {
        int breakpoint = 1;
    }
}