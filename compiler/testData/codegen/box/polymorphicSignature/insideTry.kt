// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object O {
    fun main() {}
}

fun box(): String {
    try {
        val mh = MethodHandles.lookup().findVirtual(O::class.java, "main", MethodType.methodType(Void.TYPE))
        mh.invokeExact(O)
    } finally {}
    return "OK"
}
