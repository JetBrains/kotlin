// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class Ann

class A {
    fun @receiver:Ann String.f(): String = ""

    val @receiver:Ann String?.p: String
        get() = ""

}

fun @receiver:Ann String?.topLevelF(): String = ""

val @receiver:Ann String.topLevelP: String
    get() = ""
