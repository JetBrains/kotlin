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
    a.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    a.<!RETURN_VALUE_NOT_USED!>toString<!>()
    b.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    b.<!RETURN_VALUE_NOT_USED!>toString<!>()
}

fun t(c: C, d: D) {
    c.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    c.<!RETURN_VALUE_NOT_USED!>toString<!>()
    d.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    d.<!RETURN_VALUE_NOT_USED!>toString<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, override, stringLiteral */
