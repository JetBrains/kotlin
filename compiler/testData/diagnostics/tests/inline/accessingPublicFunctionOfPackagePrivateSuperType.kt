// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81262
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
// FIR_IDENTICAL
// FILE: Super.java
class Super {
    public void foo() {}
}

// FILE: J.java
public class J extends Super {
}

// FILE: test.kt
internal <!NOTHING_TO_INLINE!>inline<!> fun foo(j: J) {
    j.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
