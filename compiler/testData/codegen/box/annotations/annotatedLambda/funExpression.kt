// TARGET_BACKEND: JVM

// WITH_STDLIB

import java.lang.reflect.Method
import kotlin.test.assertEquals

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val x: String)

fun foo0(block: () -> Unit) = block.javaClass
fun foo1(block: (String) -> Unit) = block.javaClass

fun testMethod(method: Method, name: String) {
    assertEquals("OK", method.getAnnotation(Ann::class.java).x, "On method of test named `$name`")

    for ((index, annotations) in method.getParameterAnnotations().withIndex()) {
        val ann = annotations.filterIsInstance<Ann>().single()
        assertEquals("OK$index", ann.x, "On parameter $index of test named `$name`")
    }
}

fun testClass(clazz: Class<*>, name: String) {
    val invokes = clazz.getDeclaredMethods().single() { !it.isBridge() }
    testMethod(invokes, name)
}

fun box(): String {
    testClass(foo0( @Ann("OK") fun() {} ), "1")
    testClass(foo1( @Ann("OK") fun(@Ann("OK0") x: String) {} ), "2")
    return "OK"
}
