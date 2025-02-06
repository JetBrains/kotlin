// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""
}

fun toplvl(): String = ""

fun foo(): String {
    <!RETURN_VALUE_NOT_USED!>Lib()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().getStuff()<!>
    <!RETURN_VALUE_NOT_USED!>toplvl()<!>
    return Lib().getStuff()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    <!RETURN_VALUE_NOT_USED!>Lib()<!>
    <!RETURN_VALUE_NOT_USED!>Lib().getStuff()<!>
    foo()
    toplvl()
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar()<!>
    val x = bar()
}
