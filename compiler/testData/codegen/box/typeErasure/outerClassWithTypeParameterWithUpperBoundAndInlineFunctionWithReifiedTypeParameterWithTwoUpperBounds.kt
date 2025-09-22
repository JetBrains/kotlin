// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
import kotlin.reflect.typeOf

class Pair<A, B>(val x: A, val y: B)

class A<T : CharSequence>(val a: T) {
    inline fun <reified F> typeOfX(b: F) = typeOf<F>()
    inline fun <reified F> typeOfPair(b: F) where F : CharSequence, F : Comparable<F> = typeOfX(Pair(a, b))
}

fun box() : String {
   val pair = A("0123456789").typeOfPair("").toString()
    if (pair != "Pair<T, kotlin.String>" &&
        // JS_IR, JS_IR_ES6
        pair != "Pair<T, String>")
        return "FAIL: $pair"
    return "OK"
}