// RUN_PIPELINE_TILL: FRONTEND
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
    j.<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
