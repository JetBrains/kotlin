// TARGET_BACKEND: JVM

// MODULE: lib

// FILE: test/UI.java
package test;
public class UI {
    public static String foo() {
        return "OK";
    }
}

// FILE: Parent.java
public class Parent {
    public String getUI() { return "fail"; }
}

// MODULE: main(lib)
// FILE: main.kt

import test.UI;

class Derived : Parent() {
    fun bar(): String = UI.foo()
}

fun box(): String {
    return Derived().bar()
}
