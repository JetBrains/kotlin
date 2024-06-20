// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun blackhole(vararg a: Any?) {}

val spilledVariables = mutableSetOf<Pair<String, String>>()

var c: Continuation<Unit>? = null

suspend fun saveSpilledVariables() = suspendCoroutineUninterceptedOrReturn<Unit> { continuation ->
    spilledVariables.clear()
    for (field in continuation.javaClass.declaredFields) {
        if (field.name != "label" && (field.name.length != 3 || field.name[1] != '$')) continue
        field.isAccessible = true
        val fieldValue = when (val obj = field.get(continuation)) {
            is Array<*> -> obj.joinToString(prefix = "[", postfix = "]")
            else -> obj
        }
        spilledVariables += field.name to "$fieldValue"
    }
    c = continuation
    COROUTINE_SUSPENDED
}

suspend fun test(check: Int) {
    when (check) {
        0 -> {
            val a = "a0"
            saveSpilledVariables()
            blackhole(a)
        }
        1 -> {
            val a = "a1"
            val b = "b1"
            saveSpilledVariables()
            blackhole(a, b)
        }
        else -> {
            val a = "a2"
            val b = "b2"
            val c = 1
            saveSpilledVariables()
            blackhole(a, b, c)
        }
    }
    saveSpilledVariables()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        test(0)
    }

    if (spilledVariables != setOf("label" to "1", "I$0" to "0", "I$1" to "0", "L$0" to "a0", "L$1" to "null"))
        return "FAIL 1: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "0", "I$1" to "0", "L$0" to "a0", "L$1" to "null"))
        return "FAIL 2: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "0", "I$1" to "0", "L$0" to "a0", "L$1" to "null"))
        return "FAIL 3: $spilledVariables"

    builder {
        test(1)
    }

    if (spilledVariables != setOf("label" to "2", "I$0" to "1", "I$1" to "0", "L$0" to "a1", "L$1" to "b1"))
        return "FAIL 4: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "1", "I$1" to "0", "L$0" to "a1", "L$1" to "b1"))
        return "FAIL 5: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "1", "I$1" to "0", "L$0" to "a1", "L$1" to "b1"))
        return "FAIL 6: $spilledVariables"

    builder {
        test(2)
    }

    if (spilledVariables != setOf("label" to "3", "I$0" to "2", "I$1" to "1", "L$0" to "a2", "L$1" to "b2"))
        return "FAIL 7: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "2", "I$1" to "1", "L$0" to "a2", "L$1" to "b2"))
        return "FAIL 8: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "4", "I$0" to "2", "I$1" to "1", "L$0" to "a2", "L$1" to "b2"))
        return "FAIL 9: $spilledVariables"

    return "OK"
}