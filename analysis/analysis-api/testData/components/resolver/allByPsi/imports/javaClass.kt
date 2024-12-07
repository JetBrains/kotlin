// FILE: JavaClass.java
package one.two;

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

import one.two.JavaClass.foo
import one.two.JavaClass.bar

import one.two.JavaClass.NestedClass
import one.two.JavaClass.NestedClass.foo
import one.two.JavaClass.NestedClass.bar
