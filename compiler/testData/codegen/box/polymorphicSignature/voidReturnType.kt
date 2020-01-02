// !LANGUAGE: +PolymorphicSignature
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// SKIP_JDK6
// WITH_RUNTIME

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

var state = "Fail"

class C {
    fun foo(s: String) {
        state = s
    }
}

fun box(): String {
    val mh = MethodHandles.lookup().findVirtual(
        C::class.java, "foo",
        MethodType.methodType(Void.TYPE, String::class.java)
    )

    mh.invokeExact(C(), "OK")

    return state
}
