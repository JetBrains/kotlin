// WITH_RUNTIME
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JS_IR
// IGNORE_LIGHT_ANALYSIS

val unsigned = 0x8fffffffU
val good = "123 " + unsigned
val bad = "123 " + 0x8fffffffU

fun box(): String {
    if (good != "123 2415919103") return "good: '$good'"
    if (bad != "123 2415919103") return "bad: '$bad'"
    return "OK"
}