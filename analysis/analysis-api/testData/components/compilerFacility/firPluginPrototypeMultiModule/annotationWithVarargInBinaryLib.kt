// DUMP_IR

// MODULE: binLib1
// MODULE_KIND: LibraryBinary
// FILE: SuppressLint.java
package p1;

public @interface SuppressLint {
    String[] value();
}

// MODULE: binLib2(binLib1)
// MODULE_KIND: LibraryBinary
// FILE: Base.java
package p2;

import p1.SuppressLint;

public class Base {
    public void setContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) int viewId) {}
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
    fun test() {
        val x = foo(7)
    }
}