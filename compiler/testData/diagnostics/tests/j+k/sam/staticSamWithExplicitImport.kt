// FILE: a/Statics.java

package a;

public class Statics {
    public static void foo(Runnable r) {}
}

// FILE: test.kt

package b;

import a.Statics.foo

fun test() {
    foo {}
}