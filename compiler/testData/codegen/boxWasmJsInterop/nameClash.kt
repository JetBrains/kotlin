// IGNORE_BACKEND: JS_IR, JS
// FILE: file1.kt
private fun clashName(): Int =
    js("42")

public fun getClashName1(): Int = clashName()

// FILE: file2.kt
private fun clashName(): Int =
    js("24")

public fun getClashName2(): Int = clashName()

fun box(): String {

    if (getClashName1() != 42) return "Error1"
    if (getClashName2() != 24) return "Error2"


    return "OK"
}