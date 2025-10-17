// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.reflect.typeOf

inline fun <reified T1> gBefore(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> fBefore(x: T2, y: T3) = gBefore(Pair(x, y))

fun box(): String {
    val arguments = listOf<Any?>(0, "", null, true, Pair(42, 4.2))
    for (arg in arguments) {
        val before = fBefore("1", arg).toString()
        val after = fAfter("1", arg).toString()
        if (before != "kotlin.Pair<kotlin.String, T3>" &&
            after != "kotlin.Pair<kotlin.String, T3>" &&
            // JS_IR, JS_IR_ES6
            before != "Pair<String, T3>" &&
            after != "Pair<String, T3>")
            return "FAIL: $before, $after"
    }
    return "OK"
}

inline fun <reified T1> gAfter(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> fAfter(x: T2, y: T3) = gAfter(Pair(x, y))