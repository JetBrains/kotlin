// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
// ISSUE: KT-70052
import kotlin.reflect.typeOf

class Pair<A, B>(val x: A, val y: B)

inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))

fun box() : String {
    val arguments = listOf<Any?>(0, "", null, true, Pair(42, 4.2))
    for (arg in arguments) {
        val pair = typeOfPair("1", arg).toString()
        if (pair != "Pair<kotlin.String, T3>" &&
            // JS_IR, JS_IR_ES6
            pair != "Pair<String, T3>")
            return "FAIL: $pair"
    }
    return "OK"
}