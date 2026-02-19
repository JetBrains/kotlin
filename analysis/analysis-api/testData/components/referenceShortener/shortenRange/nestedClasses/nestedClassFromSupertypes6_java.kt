// FILE: main.kt
package test

import dependency.JavaBaseClass

fun JavaBaseClass.foo() {
    <expr>val prop: JavaBaseClass.Nested = JavaBaseClass.Nested()</expr>
}

// FILE: dependency/JavaBaseClass.java
package dependency;

public class JavaBaseClass {
    public static class Nested {}
}