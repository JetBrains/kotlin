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
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    <!RETURN_VALUE_NOT_USED!>A().prop<!>
    E.A.foo()
    <!RETURN_VALUE_NOT_USED!>E.A<!>
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
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    <!RETURN_VALUE_NOT_USED!>A().prop<!>
    E.A.foo()
    <!RETURN_VALUE_NOT_USED!>E.A<!>
    foo()
    return ""
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar()<!>
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    val x = bar()
}
