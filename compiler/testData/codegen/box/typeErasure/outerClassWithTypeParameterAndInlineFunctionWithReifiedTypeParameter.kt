// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
import kotlin.reflect.typeOf

class Pair<A, B>(val x: A, val y: B)

class A<T>(val a: T) {
    inline fun <reified F> typeOfX(b: F) = typeOf<F>()
    inline fun <reified F> typeOfPair(b: F) = typeOfX(Pair(a, b))
}

fun box() : String {
    val arguments = listOf<Any?>(0, "", null, Pair(42, 4.2))
    for (arg in arguments) {
        val pair = A("1").typeOfPair(arg).toString()
        if (pair != "Pair<T, kotlin.Any?>" &&
            // JS_IR, JS_IR_ES6
            pair != "Pair<T, Any?>")
            return "FAIL: $pair"
    }
    return "OK"
}