// TARGET_BACKEND: JS_IR
// WITH_STDLIB
// KT-21235: compile-time folding uses JVM numeric formatting, which differs from JS runtime.

const val doubleText = 1.0.toString()
const val floatText = 1.0f.toString()
const val negativeZeroText = (-0.0).toString()

private fun runtimeDouble(value: Double): String = value.toString()
private fun runtimeFloat(value: Float): String = value.toString()

private fun <T> T.id(): T = this

fun box(): String {
    if (doubleText.id() != "1.0") return "Fail folded double"
    if (runtimeDouble(1.0) != "1") return "Fail runtime double"
    if (floatText.id() != "1.0") return "Fail folded float"
    if (runtimeFloat(1.0f) != "1") return "Fail runtime float"
    if (negativeZeroText.id() != "-0.0") return "Fail folded negative zero"
    if (runtimeDouble(-0.0) != "0") return "Fail runtime negative zero"
    return "OK"
}
