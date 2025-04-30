// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""
}

fun toplvl(): String = ""

@MustUseReturnValue
class A {
    fun alreadyApplied(): String = ""
}

fun foo(): String {
    <!RETURN_VALUE_NOT_USED!>Lib()<!> // TBD: we report all constructors by default
    Lib().getStuff()
    toplvl()
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    return Lib().getStuff()
}

// MODULE: main(lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

fun bar(): String {
    <!RETURN_VALUE_NOT_USED!>Lib()<!> // TBD: we report all constructors by default
    Lib().getStuff()
    foo()
    toplvl()
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar()<!>
    <!RETURN_VALUE_NOT_USED!>A().alreadyApplied()<!>
    val x = bar()
}
