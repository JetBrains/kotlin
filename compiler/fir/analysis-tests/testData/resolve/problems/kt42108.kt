// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-42108

// KT-42108: NOTHING_TO_OVERRIDE instead of CONFLICTING_OVERLOADS when declarations have different type parameter count

class E

interface Base {
    fun <T> foo(e: E)
}

interface Derived : Base {
    <!NOTHING_TO_OVERRIDE!>override<!> <!CONFLICTING_OVERLOADS!>fun <T, R> foo(e: E)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, override,
typeParameter */
