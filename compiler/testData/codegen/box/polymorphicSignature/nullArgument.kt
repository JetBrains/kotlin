// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// FULL_JDK
// SKIP_JDK6
// WITH_STDLIB

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

var result: String? = "Fail"

class C {
    fun foo(s: Nothing?) {
        result = s
    }
}

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "foo",
        MethodType.methodType(Void.TYPE, Void::class.java)
    )

    mh.invokeExact(C(), null)

    return result ?: "OK"
}
