// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// JSPECIFY_STATE: strict

// FILE: sandbox/module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox/test/package-info.java
@NullMarked
package test;

import org.jspecify.annotations.NullMarked;

// FILE: sandbox/test/Test.java
package test;

public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    x.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}