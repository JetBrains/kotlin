// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }
}

fun toplvl(): String = ""

@MustUseReturnValue
class A {
    fun alreadyApplied(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }
}

enum class E {
    A, B;
    fun foo() = ""
}

fun foo(): String {
    Lib()
    Lib().getStuff()
    Lib().prop
    Lib().prop = ""
    toplvl()
    A().<!RETURN_VALUE_NOT_USED!>alreadyApplied<!>()
    A().<!RETURN_VALUE_NOT_USED!>prop<!>
    E.A.foo()
    E.<!RETURN_VALUE_NOT_USED!>A<!>
    return Lib().getStuff()
}

// MODULE: main(lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

fun bar(): String {
    Lib()
    Lib().getStuff()
    Lib().prop
    Lib().prop = ""
    toplvl()
    A().<!RETURN_VALUE_NOT_USED!>alreadyApplied<!>()
    A().<!RETURN_VALUE_NOT_USED!>prop<!>
    E.A.foo()
    E.<!RETURN_VALUE_NOT_USED!>A<!>
    foo()
    return ""
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar<!>()
    A().<!RETURN_VALUE_NOT_USED!>alreadyApplied<!>()
    val x = bar()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
getter, localProperty, propertyDeclaration, setter, stringLiteral */
