// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// CODE_ANALYSIS_STATE warn
// FILE: A.java

import codeanalysis.annotations.*;

public class A<T> {
    public void foo(T t) {}

    @DefaultNotNull
    public void bar(String s, T t) {} // t should not become not nullable
}

// FILE: main.kt

fun main(a1: A<Int>, a2: A<Int?>) {
    a1.foo(null)
    a1.foo(1)

    a2.foo(null)
    a2.foo(1)

    a1.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, null)
    a1.bar("", null)
    a1.bar("", 1)

    a2.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, null)
    a2.bar("", null)
    a2.bar("", 1)
}
