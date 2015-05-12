import java.lang.annotation.*
import java.lang.reflect.Method
import kotlin.reflect.jvm.java
import kotlin.test.assertEquals

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(val x: String)

fun testMethod(method: Method, name: String) {
    assertEquals("OK", method.getAnnotation(javaClass<Ann>()).x, "On method of test named `$name`")

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
    testClass(Test.apply(@Ann("OK") {}), "1")
    testClass(Test.apply @Ann("OK") {}, "2")
    return "OK"
}
