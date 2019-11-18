// IGNORE_BACKEND_FIR: JVM_IR
// TODO: investigate should it be ran for JS or not
// TARGET_BACKEND: JVM

// WITH_REFLECT

// FILE: J.java
public class J {
    private final int param;

    public J(int param) {
        this.param = param;
    }

    public String foo(int[] arr, Object[] arr2, Integer y) {
        return "" + param + arr[0] + arr2[0] + y;
    }
}

// FILE: K.kt
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val f = J(0)::foo
    assertEquals(
            listOf(IntArray::class.java, Array<Any>::class.java, Integer::class.java),
            f.parameters.map { it.type.javaType }
    )
    assertEquals(String::class.java, f.returnType.javaType)

    assertEquals("01A2", f.call(intArrayOf(1), arrayOf("A"), 2))

    return "OK"
}
