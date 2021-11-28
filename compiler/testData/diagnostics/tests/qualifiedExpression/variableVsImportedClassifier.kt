// FIR_IDENTICAL
// SKIP_TXT
// FILE: test/W.java
package test;

public class W {
    public void foo() {}
    public static void bar() {}
}

// FILE: main.kt
package main1
import test.W

val W: W = W()

fun main() {
    W.foo()
    W.<!UNRESOLVED_REFERENCE!>bar<!>()
    W().foo()
}

// FILE: main2.kt
package main2
import test.W

fun main() {
    W.<!UNRESOLVED_REFERENCE!>foo<!>()
    W.bar()
    W().foo()
}
