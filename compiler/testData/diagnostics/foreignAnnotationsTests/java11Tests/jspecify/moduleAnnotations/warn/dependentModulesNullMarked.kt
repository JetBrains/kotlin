// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// MODULE: module1
// FILE: module1/module-info.java
module module1 {
}

// MODULE: module2(module1)
// FILE: module2/module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
module module2 {
    requires java9_annotations;
    requires module1;
    exports test2;
}

// FILE: module2/test2/Test.java
package test2;

public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
fun main(y: test2.Test) {
    y.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}