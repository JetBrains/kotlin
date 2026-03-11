// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

// MODULE: binLib1
// MODULE_KIND: LibraryBinary
// FILE: MyJavaAnnotation.java
package p1;

public @interface MyJavaAnnotation {
    String[] value();
}

// FILE: MyKotlinAnnotation.kt
package p1

annotation class MyKotlinAnnotation(val value: Array<String>)

// MODULE: binLib2(binLib1)
// MODULE_KIND: LibraryBinary
// FILE: Base.java
package p2;

import p1.MyJavaAnnotation;
import p1.MyKotlinAnnotation;

public class Base {
    public void setContentView(@MyJavaAnnotation({"One", "Two"}) @MyKotlinAnnotation({"Three", "Four"}) int viewId) {}
}

// MODULE: binLib3(binLib2)
// MODULE_KIND: LibraryBinary
// FILE: foo.kt
package p3

import p2.Base

public fun Base.foo(b: Int): Int {
    setContentView(b)
    return b + 10
}

// MODULE: main(binLib1, binLib2, binLib3)
// FILE: main.kt
package home

import p2.Base
import p3.foo

class Child: Base() {
    fun test(): String {
        val x = foo(7)
        return "OK"
    }
}

fun box() = Child().test()
