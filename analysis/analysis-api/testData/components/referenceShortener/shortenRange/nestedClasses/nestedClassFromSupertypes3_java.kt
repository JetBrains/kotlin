// FILE: main.kt
package test

import dependency.JavaBaseClass

class Foo : JavaBaseClass() {
    <expr>val prop: JavaBaseClass.Nested = JavaBaseClass.Nested()</expr>
}

// FILE: dependency/JavaBaseClass.java
package dependency;

public class JavaBaseClass {
    public static class Nested {}
}