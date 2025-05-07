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
    <!RETURN_VALUE_NOT_USED!>Lib()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().getStuff()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().prop<!>
    Lib().prop = ""
    <!RETURN_VALUE_NOT_USED!>toplvl()<!>
    <!RETURN_VALUE_NOT_USED!>E.A<!>
    <!RETURN_VALUE_NOT_USED!>E.A.foo()<!>
    return Lib().getStuff()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    <!RETURN_VALUE_NOT_USED!>Lib()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().getStuff()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().prop<!>
    Lib().prop = ""
    <!RETURN_VALUE_NOT_USED!>foo()<!>
    <!RETURN_VALUE_NOT_USED!>toplvl()<!>
    <!RETURN_VALUE_NOT_USED!>E.A<!>
    <!RETURN_VALUE_NOT_USED!>E.A.foo()<!>
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar()<!>
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    val x = bar()
}
