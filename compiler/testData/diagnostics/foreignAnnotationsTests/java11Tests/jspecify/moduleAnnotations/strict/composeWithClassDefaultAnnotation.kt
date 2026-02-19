// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// ALLOW_KOTLIN_PACKAGE

// FILE: sandbox/module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox/test/Test.java
package test;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    x.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
