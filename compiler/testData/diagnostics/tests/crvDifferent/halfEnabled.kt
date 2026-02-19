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

@MustUseReturnValues
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
    A().alreadyApplied()
    A().prop
    E.A.foo()
    E.A
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
    A().alreadyApplied()
    A().prop
    E.A.foo()
    E.A
    foo()
    return ""
}

fun main() {
    bar()
    A().alreadyApplied()
    val x = bar()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
getter, localProperty, propertyDeclaration, setter, stringLiteral */
