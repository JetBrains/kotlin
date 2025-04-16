// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP

// MODULE: lib1
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""
}

fun toplvl(): String = ""

@MustUseReturnValue
fun alreadyApplied(): String = ""

fun foo(): String {
    Lib()
    Lib().getStuff()
    toplvl()
    alreadyApplied()
    return Lib().getStuff()
}

// MODULE: main(lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

fun bar(): String {
    Lib()
    Lib().getStuff()
    foo()
    toplvl()
    return foo()
}

fun main() {
    bar()
    alreadyApplied()
    val x = bar()
}
