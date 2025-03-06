// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS

// WITH_STDLIB
// FILE: Test.java

class Test {
    public static Class<?> apply(Runnable x) {
        return x.getClass();
    }

    public static interface ABC {
        void apply(String x1, String x2);
    }

    public static Class<?> applyABC(ABC x) {
        return x.getClass();
    }
}

// FILE: samFunReference.kt

import java.lang.reflect.Method
import kotlin.test.assertEquals

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val x: String)

fun testMethodNoAnnotations(method: Method, name: String) {
    assertEquals(0, method.getDeclaredAnnotations().size, "No method annotations expected `$name`")
    for ((index, annotations) in method.getParameterAnnotations().withIndex()) {
        assertEquals(0, annotations.size, "No parameter $index annotations expected `$name`")
    }
}

fun testClass(clazz: Class<*>, name: String) {
    val invokes = clazz.getDeclaredMethods().single() { !it.isBridge() }
    testMethodNoAnnotations(invokes, name)
}

@Ann("OK")
fun annotatedABC(@Ann("OK0") x: String, @Ann("OK1") y: String) {}

@Ann("OK")
fun annotatedRunnable() {}

fun box(): String {
    testClass(Test.applyABC(::annotatedABC), "1")
    testClass(Test.apply(::annotatedRunnable), "2")
    val abcReference = ::annotatedABC
    testClass(Test.applyABC(abcReference), "3")
    val runnableReference = ::annotatedRunnable
    testClass(Test.apply(runnableReference), "4")
    return "OK"
}
