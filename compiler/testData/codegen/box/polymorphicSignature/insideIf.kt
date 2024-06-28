// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object O {
    fun main() {}
}

fun f() = true

fun box(): String {
    if (f()) {
        val mh = MethodHandles.lookup().findVirtual(O::class.java, "main", MethodType.methodType(Void.TYPE))
        mh.invokeExact(O)
    } else {}
    return "OK"
}
