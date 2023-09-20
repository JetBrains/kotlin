// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// MODULE: module1
// FILE: module1/module-info.java
import org.jspecify.nullness.NullMarked;

@NullMarked
module module1 {
    requires java9_annotations;
    exports test1;
}

// FILE: module1/test1/Test1.java
package test1;

public class Test1 {
    public void foo(Integer x) {}
}

// MODULE: module2(module1)
// FILE: module2/module-info.java
module module2 {
    requires module1;
    exports test2;
}

// FILE: module2/test2/Test2.java
package test2;

import test1.Test1;

public class Test2 extends Test1 {
    public void foo2(Integer x) {}
}

// FILE: main.kt
import test1.Test1
import test2.Test2

fun main(x: Test1, y: Test2) {
    x.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    y.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    y.foo2(null)
}