// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.*
import kotlin.test.*

class C {
    companion object {
        @JvmStatic
        fun foo(s: String): Int = s.length
    }
}

fun box(): String {
    val foo = C.Companion::class.members.single { it.name == "foo" } as KFunction<*>

    val j = foo.javaMethod ?: return "Fail: no Java method found for C::foo"
    assertEquals(3, j.invoke(C, "abc"))

    val k = j.kotlinFunction ?: return "Fail: no Kotlin function found for Java method C::foo"
    assertEquals(3, k.call(C, "def"))


    val staticMethod = C::class.java.getDeclaredMethod("foo", String::class.java)
    val k2 = staticMethod.kotlinFunction ?:
             return "Fail: no Kotlin function found for static bridge for @JvmStatic method in companion object C::foo"
    assertEquals(3, k2.call(C, "ghi"))

    assertFailsWith(NullPointerException::class) { k2.call(null, "")!! }

    val j2 = k2.javaMethod
    assertEquals(j, j2)

    return "OK"
}
