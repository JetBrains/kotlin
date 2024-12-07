// FILE: JavaClass.java
public class JavaClass {
    public static class NestedClass {
        public static void foo() { }
        public static int bar = 1;
    }

    public static void foo() { }
    public static int bar = 1;
}

// FILE: main.kt
package main

import JavaClass.foo
import JavaClass.bar

import JavaClass.NestedClass
import JavaClass.NestedClass.foo
import JavaClass.NestedClass.bar
