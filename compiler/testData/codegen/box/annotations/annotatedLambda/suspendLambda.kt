// TARGET_BACKEND: JVM
// WITH_STDLIB
// Only implemented in the IR backend.
// IGNORE_BACKEND: JVM

import java.lang.reflect.Method
import kotlin.test.assertEquals

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val x: String)

fun foo0(block: suspend () -> Unit) = block.javaClass

fun testHasAnnotation(method: Method, name: String) {
    assertEquals(
        "OK",
        method.getAnnotation(Ann::class.java)?.x,
        "Missing or incorrect annotation on method `${method.name}` of test named `$name`"
    )
}

fun testDoesNotHaveAnnotation(method: Method, name: String) {
    assertEquals(
        null,
        method.getAnnotation(Ann::class.java),
        "Unexpected annotation on method `${method.name}` of test named `$name`"
    )
}

fun testNonTailCallSuspendLambda(clazz: Class<*>, name: String) {
    // Check that non-bridge `invokeSuspend` contains the suspend lambda annotation.
    val invokeSuspends = clazz.getDeclaredMethods().filter { !it.isBridge() && it.name == "invokeSuspend" }
    invokeSuspends.forEach { testHasAnnotation(it, name) }
    // Check that non-bridge `invoke` does not contain the suspend lambda annotation.
    val invokes = clazz.getDeclaredMethods().filter { !it.isBridge() && it.name == "invoke" }
    invokes.forEach { testDoesNotHaveAnnotation(it, name) }
}

fun testTailCallSuspendLambda(clazz: Class<*>, name: String) {
    val invokeSuspends = clazz.getDeclaredMethods().filter { !it.isBridge() && it.name == "invokeSuspend" }
    if (invokeSuspends.isNotEmpty()) {
        error("$clazz contains invokeSuspend")
    }
    // Check that non-bridge `invoke` contains the suspend lambda annotation.
    val invokes = clazz.getDeclaredMethods().filter { !it.isBridge() && it.name == "invoke" }
    invokes.forEach { testHasAnnotation(it, name) }
}

suspend fun dummy() {}

fun box(): String {
    testNonTailCallSuspendLambda(foo0(@Ann("OK") { dummy(); dummy() }), "1")
    testNonTailCallSuspendLambda(foo0() @Ann("OK") { dummy(); dummy() }, "2")

    testTailCallSuspendLambda(foo0(@Ann("OK") { }), "1")
    testTailCallSuspendLambda(foo0() @Ann("OK") { }, "2")
    return "OK"
}
