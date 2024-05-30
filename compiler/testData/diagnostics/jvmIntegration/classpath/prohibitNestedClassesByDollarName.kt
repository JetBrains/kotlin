// FIR_IDENTICAL
// MODULE: library
// FILE: test/JavaOuter.java
package test;

public class JavaOuter {
    public static class JavaNested {}
}

// FILE: test/Outer.kt
package test

class Outer {
    class Nested
}

// MODULE: main(library)
// FILE: main.kt
import test.Outer
import test.JavaOuter

fun main(args: Array<String>) {
    Outer.Nested()
    test.<!UNRESOLVED_REFERENCE!>`Outer$Nested`<!>()

    JavaOuter.JavaNested()
    test.<!UNRESOLVED_REFERENCE!>`JavaOuter$JavaNested`<!>()
}
