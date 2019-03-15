// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

inline fun <reified T> foo(x: Any?) = Pair(x is T, x is T?)

fun box(): String {
    val x1 = foo<Array<String>>(arrayOf(""))
    if (x1.toString() != "(true, true)") return "fail 1"

    val x2 = foo<Array<String>?>(arrayOf(""))
    if (x2.toString() != "(true, true)") return "fail 2"

    val x3 = foo<Array<String>>(null)
    if (x3.toString() != "(false, true)") return "fail 3"

    val x4 = foo<Array<String>?>(null)
    if (x4.toString() != "(true, true)") return "fail 4"

    val x5 = foo<Array<Double>?>(arrayOf(""))
    if (x5.toString() != "(false, false)") return "fail 5"

    val x6 = foo<Array<Double>?>(null)
    if (x6.toString() != "(true, true)") return "fail 6"
    return "OK"
}
