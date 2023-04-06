// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

annotation class Ann

class A {
    fun @receiver:Ann String.f(): String = ""

    val @receiver:Ann String?.p: String
        get() = ""

}

fun @receiver:Ann String?.topLevelF(): String = ""

val @receiver:Ann String.topLevelP: String
    get() = ""
