// FIR_IDENTICAL
// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: C:foo
// FILE: A.java

import org.jetbrains.annotations.*;

public class A {
    public void foo(@NotNull Integer x) {}
}

// FILE: main.kt

interface B {
    fun foo(x: Int) {}
}

class C : A(), B

fun main() {
    C().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(42)
}
