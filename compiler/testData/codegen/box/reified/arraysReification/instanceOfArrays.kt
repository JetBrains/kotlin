// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

inline fun <reified T> foo(x: Any?) = Pair(x is T, x is T?)
inline fun <reified F> bar(y: Any?) = foo<Array<F>>(y)
inline fun <reified F> barNullable(y: Any?) = foo<Array<F>?>(y)

fun box(): String {
    val x1 = bar<String>(arrayOf(""))
    if (x1.toString() != "(true, true)") return "fail 1"

    val x3 = bar<String>(null)
    if (x3.toString() != "(false, true)") return "fail 3"

    val x4 = bar<String?>(null)
    if (x4.toString() != "(false, true)") return "fail 4"

    val x5 = bar<Double?>(arrayOf(""))
    if (x5.toString() != "(false, false)") return "fail 5"

    val x6 = bar<Double?>(null)
    if (x6.toString() != "(false, true)") return "fail 6"

    // barNullable

    val x7 = barNullable<String>(arrayOf(""))
    if (x7.toString() != "(true, true)") return "fail 7"

    val x9 = barNullable<String>(null)
    if (x9.toString() != "(true, true)") return "fail 9"

    val x10 = barNullable<Double?>(arrayOf(""))
    if (x10.toString() != "(false, false)") return "fail 11"

    val x12 = barNullable<Double?>(null)
    if (x12.toString() != "(true, true)") return "fail 12"
    return "OK"
}
