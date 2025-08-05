// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib0
// RETURN_VALUE_CHECKER_MODE: DISABLED

// FILE: Base.kt

class A {
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

class B

fun t(a: A, b: B) {
    a.hashCode()
    a.toString()
    b.hashCode()
    b.toString()
}

// MODULE: main(lib0)
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: App.kt

class C {
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

class D

fun t(a: A, b: B) {
    <!RETURN_VALUE_NOT_USED!>a.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>a.toString()<!>
    <!RETURN_VALUE_NOT_USED!>b.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>b.toString()<!>
}

fun t(c: C, d: D) {
    <!RETURN_VALUE_NOT_USED!>c.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>c.toString()<!>
    <!RETURN_VALUE_NOT_USED!>d.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>d.toString()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, override, stringLiteral */
