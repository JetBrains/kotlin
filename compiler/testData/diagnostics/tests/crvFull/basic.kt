// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP

// MODULE: lib1

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
    return Lib().getStuff()
}

// MODULE: main(lib1)

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
