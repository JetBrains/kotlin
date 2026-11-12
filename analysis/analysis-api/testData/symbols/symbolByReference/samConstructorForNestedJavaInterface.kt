// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

// FILE: main.kt
package test

fun usage() {
    JavaOuter.<caret>Nested { }
}

// FILE: test/JavaOuter.java
package test;

public class JavaOuter {
    public interface Nested {
        void f();
    }
}
