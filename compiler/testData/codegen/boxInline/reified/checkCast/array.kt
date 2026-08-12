// WITH_STDLIB
// IGNORE_BACKEND: WASM_JS, WASM_WASI, JS_IR, JS_IR_ES6
// FILE: 1.kt
package test

inline fun <reified T> castToArray(x: Any?): Array<T> = x as Array<T>

inline fun <reified T> safeCastToArray(x: Any?): Array<T>? = x as? Array<T>

inline fun <reified T> isArray(x: Any?): Boolean = x is Array<T>

inline fun <reified T> castToNestedArray(x: Any?): Array<Array<T>> = x as Array<Array<T>>

// FILE: 2.kt
import test.*

fun box(): String {
    val stringArray: Any = arrayOf("a", "b", "c")
    val intArray: Any = arrayOf(1, 2, 3)

    if (!isArray<String>(stringArray)) return "Fail 1"
    if (isArray<Int>(stringArray)) return "Fail 2"

    val res1 = castToArray<String>(stringArray)
    if (res1.size != 3 || res1[0] != "a") return "Fail 3"

    try {
        castToArray<Int>(stringArray)
        return "Fail 4"
    } catch (e: ClassCastException) {
        // Expected
    }

    val res2 = safeCastToArray<String>(stringArray) ?: return "Fail 5"
    if (res2.size != 3) return "Fail 6"

    val res3 = safeCastToArray<Int>(stringArray)
    if (res3 != null) return "Fail 7"

    val nestedStringArray: Any = arrayOf(arrayOf("x"))
    if (!isArray<Array<String>>(nestedStringArray)) return "Fail 8"
    if (isArray<Array<Int>>(nestedStringArray)) return "Fail 9"

    val res4 = castToNestedArray<String>(nestedStringArray)
    if (res4[0][0] != "x") return "Fail 10"

    return "OK"
}
