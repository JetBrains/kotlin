// IGNORE_BACKEND: JS_IR, JS_IR_ES6, JS
// FILE: file1.kt
@JsFun("() => 42")
private external fun clashName(): Int

public fun getClashName1(): Int = clashName()

// FILE: file2.kt
@JsFun("() => 24")
private external fun clashName(): Int

public fun getClashName2(): Int = clashName()

fun box(): String {

    if (getClashName1() != 42) return "Error1"
    if (getClashName2() != 24) return "Error2"


    return "OK"
}