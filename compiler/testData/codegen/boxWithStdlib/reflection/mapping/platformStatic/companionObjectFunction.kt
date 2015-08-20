import kotlin.platform.platformStatic as static
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import kotlin.test.failsWith

class C {
    companion object {
        @static fun foo(s: String): Int = s.length()
    }
}

fun box(): String {
    val foo = C.Companion::foo

    val j = foo.javaMethod ?: return "Fail: no Java method found for C::foo"
    assertEquals(3, j.invoke(C, "abc"))

    val k = j.kotlinFunction ?: return "Fail: no Kotlin function found for Java method C::foo"
    assertEquals(3, k.call(C, "def"))


    val staticMethod = javaClass<C>().getDeclaredMethod("foo", javaClass<String>())
    val k2 = staticMethod.kotlinFunction ?:
             return "Fail: no Kotlin function found for static bridge for @jvmStatic method in companion object C::foo"
    assertEquals(3, k2.call(C, "ghi"))

    failsWith(javaClass<NullPointerException>()) { k2.call(null, "")!! }

    val j2 = k2.javaMethod
    assertEquals(j, j2)

    return "OK"
}
