// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// IGNORE_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_REFLECTION
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.reflect.typeOf

private inline fun <reified T> gBefore() = typeOf<List<T>>()
private inline fun <U : Number> fBefore() = gBefore<List<U>>()

fun box(): String {
    val before = fBefore<Int>().toString()
    val after = fAfter<Int>().toString()
    if (before != "kotlin.collections.List<kotlin.collections.List<U>>" &&
        after != "kotlin.collections.List<kotlin.collections.List<U>>" &&
        // JS_IR, JS_IR_ES6
        before != "List<List<U>>" &&
        after != "List<List<U>>" &&
        // JVM_IR
        before != "java.util.List<java.util.List<U>> (Kotlin reflection is not available)" &&
        after != "java.util.List<java.util.List<U>> (Kotlin reflection is not available)")
        return "FAIL: $before, $after"
    return "OK"
}

private inline fun <reified T> gAfter() = typeOf<List<T>>()
private inline fun <U : Number> fAfter() = gAfter<List<U>>()