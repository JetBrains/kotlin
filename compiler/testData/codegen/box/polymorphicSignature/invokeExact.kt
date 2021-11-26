// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class C {
    fun foo(s: String, d: Double, x: Int): String = "$s$d$x"

    companion object {
        @JvmStatic
        fun bar(): Any = "OK"
    }
}

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "foo",
        MethodType.methodType(String::class.java, String::class.java, Double::class.java, Int::class.java)
    )
    val result: String = mh.invokeExact(C(), "Hello", 0.01, 42) as String
    if (result != "Hello0.0142") return "Fail 1: $result"

    val mh2 = MethodHandles.lookup().findStatic(C::class.java, "bar", MethodType.methodType(Object::class.java))
    val result2 = mh2.invokeExact() is String
    return if (result2) "OK" else "Fail 2"
}
