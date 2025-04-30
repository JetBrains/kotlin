// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""
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
    Lib()
    Lib().getStuff()
    toplvl()
    E.A
    E.A.foo()
    return Lib().getStuff()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    Lib()
    Lib().getStuff()
    foo()
    toplvl()
    E.A // TODO: either add metadata flag or always report enum entry access
    E.A.foo()
    return foo()
}

fun main() {
    bar()
    A().alreadyApplied()
    val x = bar()
}
