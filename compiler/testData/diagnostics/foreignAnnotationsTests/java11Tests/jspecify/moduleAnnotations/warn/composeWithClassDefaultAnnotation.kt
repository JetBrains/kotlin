// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// FILE: sandbox/test/module-info.java
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
    x.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}