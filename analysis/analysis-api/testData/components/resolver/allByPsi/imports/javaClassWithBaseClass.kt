// FILE: BaseClass.java
package one.two;

public class BaseClass {
    public static class NestedClass {
        public static void foo() { }
        public static int bar = 1;
    }

    public static void baseFoo() { }
    public static int baseBar = 1;
}

// FILE: JavaClass.java
package one.two;

public class JavaClass extends BaseClass {
    public static void foo() { }
    public static int bar = 1;
}

// FILE: main.kt
package main

import one.two.BaseClass.baseFoo
import one.two.BaseClass.baseBar

import one.two.BaseClass.NestedClass
import one.two.BaseClass.NestedClass.foo
import one.two.BaseClass.NestedClass.bar

import one.two.JavaClass.baseFoo
import one.two.JavaClass.baseBar
import one.two.JavaClass.foo
import one.two.JavaClass.bar
