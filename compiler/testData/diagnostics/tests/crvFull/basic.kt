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

@MustUseReturnValues
class A {
    fun alreadyApplied(): String = ""
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
    E.A
    E.A.foo()
    return Lib().getStuff()
}

fun withLocal() {
    fun local(): String = ""
    local()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    Lib()
    Lib().getStuff()
    Lib().prop
    Lib().prop = ""
    foo()
    toplvl()
    E.A
    E.A.foo()
    return foo()
}

fun main() {
    bar()
    A().alreadyApplied()
    val x = bar()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
getter, localFunction, localProperty, propertyDeclaration, setter, stringLiteral */
