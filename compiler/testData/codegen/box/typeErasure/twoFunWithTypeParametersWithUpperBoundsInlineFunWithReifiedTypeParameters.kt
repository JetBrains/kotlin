// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT

// FILE: lib.kt
import kotlin.reflect.typeOf

inline fun <reified T> gBefore() = typeOf<List<T>>()
inline fun <U : Number> fBefore() = gBefore<List<U>>()

inline fun <reified T> gAfter() = typeOf<List<T>>()
inline fun <U : Number> fAfter() = gAfter<List<U>>()

// FILE: main.kt
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.reflect.typeOf

fun box(): String {
    val before = fBefore<Int>().toString()
    val after = fAfter<Int>().toString()
    if (before != "kotlin.collections.List<kotlin.collections.List<U>>" &&
        after != "kotlin.collections.List<kotlin.collections.List<U>>" &&
        // JS_IR, JS_IR_ES6
        before != "List<List<U>>" &&
        after != "List<List<U>>")
        return "FAIL: $before, $after"
    return "OK"
}
