// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// JSPECIFY_STATE: strict

// FILE: sandbox/test/module-info.java
import org.jspecify.nullness.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox/test/Test.java
package test;

import org.jspecify.nullness.Nullable;

public class Test {
    public void foo(@Nullable Integer x) {}
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    x.foo(null)
}
