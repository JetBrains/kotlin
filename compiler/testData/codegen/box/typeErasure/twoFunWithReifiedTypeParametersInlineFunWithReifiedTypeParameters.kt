// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// WITH_REFLECTION
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.reflect.typeOf

private inline fun <reified T> gBefore() = typeOf<List<T>>()
private inline fun <reified U> fBefore() = gBefore<List<U>>()

fun box(): String {
    val before = fBefore<Int>().toString()
    val after = fAfter<Int>().toString()
    if (before != "kotlin.collections.List<kotlin.collections.List<kotlin.Int>>" &&
        after != "kotlin.collections.List<kotlin.collections.List<kotlin.Int>>" &&
        // JS_IR, JS_IR_ES6
        before != "List<List<Int>>" &&
        after != "List<List<Int>>" &&
        // JVM_IR
        before != "java.util.List<java.util.List<java.lang.Integer>> (Kotlin reflection is not available)" &&
        after != "java.util.List<java.util.List<java.lang.Integer>> (Kotlin reflection is not available)")
        return "FAIL: $before, $after"
    return "OK"
}

private inline fun <reified T> gAfter() = typeOf<List<T>>()
private inline fun <reified U> fAfter() = gAfter<List<U>>()