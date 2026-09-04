// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

open class B {
    fun foo(l: List<String>) {}
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>fun foo(l: List<Int>)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
