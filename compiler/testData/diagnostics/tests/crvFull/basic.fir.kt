// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

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
}

enum class E {
    A, B;
    fun foo() = ""
}

fun foo(): String {
    <!RETURN_VALUE_NOT_USED!>Lib<!>()
    Lib().<!RETURN_VALUE_NOT_USED!>getStuff<!>()
    Lib().<!RETURN_VALUE_NOT_USED!>prop<!>
    Lib().prop = ""
    <!RETURN_VALUE_NOT_USED!>toplvl<!>()
    E.<!RETURN_VALUE_NOT_USED!>A<!>
    E.A.<!RETURN_VALUE_NOT_USED!>foo<!>()
    return Lib().getStuff()
}

fun withLocal() {
    fun local(): String = ""
    <!RETURN_VALUE_NOT_USED!>local<!>()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    <!RETURN_VALUE_NOT_USED!>Lib<!>()
    Lib().<!RETURN_VALUE_NOT_USED!>getStuff<!>()
    Lib().<!RETURN_VALUE_NOT_USED!>prop<!>
    Lib().prop = ""
    <!RETURN_VALUE_NOT_USED!>foo<!>()
    <!RETURN_VALUE_NOT_USED!>toplvl<!>()
    E.<!RETURN_VALUE_NOT_USED!>A<!>
    E.A.<!RETURN_VALUE_NOT_USED!>foo<!>()
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar<!>()
    A().<!RETURN_VALUE_NOT_USED!>alreadyApplied<!>()
    val x = bar()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
getter, localFunction, localProperty, propertyDeclaration, setter, stringLiteral */
