// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// FILE: my.sand.box/module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
open module my.sand.box {
    requires java9_annotations;
    exports my.test;
}

// FILE: my.sand.box/my/test/Test.java
package my.test;

public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
import my.test.Test

fun main(x: Test) {
    x.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}