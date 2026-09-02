// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun dummy() {}

fun box(): String {
    val lambda: suspend () -> Unit = {
        dummy()
        dummy()
    }

    val method = lambda.javaClass.getDeclaredMethod("invokeSuspend", Any::class.java)
    method.isAccessible = true

    var field: java.lang.reflect.Field? = null
    var clazz: Class<*>? = lambda.javaClass
    while (clazz != null) {
        try {
            field = clazz.getDeclaredField("label")
            break
        } catch (e: Exception) {
            clazz = clazz.superclass
        }
    }
    if (field == null) return "FAIL: label field not found"
    field.isAccessible = true

    // Test label > max label with Unit
    field.setInt(lambda, 999)
    try {
        method.invoke(lambda, Unit)
        return "FAIL: expected IllegalStateException for label 999"
    } catch (e: Throwable) {
        val cause = e.cause ?: e
        if (cause !is IllegalStateException || cause.message != "call to 'resume' before 'invoke' with coroutine") {
            return "FAIL: unexpected exception for label 999: $cause"
        }
    }

    // Test boundary: max label is 2 (2 suspension points), so label = 3 must fail
    field.setInt(lambda, 3)
    try {
        method.invoke(lambda, "arbitrary result")
        return "FAIL: expected IllegalStateException for boundary label 3"
    } catch (e: Throwable) {
        val cause = e.cause ?: e
        if (cause !is IllegalStateException || cause.message != "call to 'resume' before 'invoke' with coroutine") {
            return "FAIL: unexpected exception for boundary label 3: $cause"
        }
    }

    // Test label < 0 with Unit
    field.setInt(lambda, -1)
    try {
        method.invoke(lambda, Unit)
        return "FAIL: expected IllegalStateException for label -1"
    } catch (e: Throwable) {
        val cause = e.cause ?: e
        if (cause !is IllegalStateException || cause.message != "call to 'resume' before 'invoke' with coroutine") {
            return "FAIL: unexpected exception for label -1: $cause"
        }
    }

    // Test label < 0 with non-Unit result
    field.setInt(lambda, -42)
    try {
        method.invoke(lambda, "arbitrary result")
        return "FAIL: expected IllegalStateException for label -42"
    } catch (e: Throwable) {
        val cause = e.cause ?: e
        if (cause !is IllegalStateException || cause.message != "call to 'resume' before 'invoke' with coroutine") {
            return "FAIL: unexpected exception for label -42: $cause"
        }
    }

    return "OK"
}
