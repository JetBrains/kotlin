// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECT
// ISSUE: KT-48670
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.reflect.typeOf

inline fun <reified T> gBefore() = typeOf<List<T>>()
inline fun <U> fBefore() = gBefore<List<U>>()

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

inline fun <reified T> gAfter() = typeOf<List<T>>()
inline fun <U> fAfter() = gAfter<List<U>>()