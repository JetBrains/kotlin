// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
import kotlin.reflect.typeOf

class Triple<A, B, C>(val x: A, val y: B, val z: C)

inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
inline fun <reified T2, reified T3, T4> typeOfTriple1(x: T2, y: T3, z: T4) = typeOfX(Triple(x, y, z))
inline fun <reified T2, T3, reified T4> typeOfTriple2(x: T2, y: T3, z: T4) = typeOfX(Triple(x, y, z))

fun box() : String {
    val arguments = listOf<Any?>(0, "", null, true, Triple(42, 4.2, 42L))
    for (arg in arguments) {
        val triple1 = typeOfTriple1("1", 1, arg).toString()
        val triple2 = typeOfTriple2("1", arg, 1).toString()
        if (triple1 != "Triple<kotlin.String, kotlin.Int, T4>" &&
            triple2 != "Triple<kotlin.String, T3, kotlin.Int>" &&
            // JS_IR, JS_IR_ES6
            triple1 != "Triple<String, Int, T4>" &&
            triple2 != "Triple<String, T3, Int>")
            return "FAIL: $triple1, $triple2"
    }
    return "OK"
}