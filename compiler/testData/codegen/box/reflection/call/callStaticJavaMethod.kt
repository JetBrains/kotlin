// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public static String foo(int x, int[] arr, Object[] arr2) {
        return "" + x + arr[0] + arr2[0];
    }
}

// FILE: K.kt

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val f = J::foo
    assertEquals(listOf(Integer.TYPE, IntArray::class.java, Array<Any>::class.java), f.parameters.map { it.type.javaType })
    assertEquals(String::class.java, f.returnType.javaType)

    assertEquals("01A", f.call(0, intArrayOf(1), arrayOf("A")))

    return "OK"
}
