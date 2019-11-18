// !LANGUAGE: +PolymorphicSignature
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// SKIP_JDK6
// WITH_RUNTIME

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class C {
    fun foo(s: String, d: Double, x: Int): String = "$s$d$x"
}

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "foo",
        MethodType.methodType(String::class.java, String::class.java, Double::class.java, Int::class.java)
    )
    val result: String = mh.invokeExact(C(), "Hello", 0.01, 42) as String
    return if (result == "Hello0.0142") "OK" else "Fail: $result"
}
