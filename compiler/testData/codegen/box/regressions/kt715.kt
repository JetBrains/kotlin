// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.*

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
inline fun <reified T: Any> javaClass(): Class<T> = T::class.java

val test = "lala".javaClass

val test2 = javaClass<Iterator<Int>> ()

fun box(): String {
    if(test.getCanonicalName() != "java.lang.String") return "fail"
    if(test2.getCanonicalName() != "java.util.Iterator") return "fail"
    return "OK"
}
