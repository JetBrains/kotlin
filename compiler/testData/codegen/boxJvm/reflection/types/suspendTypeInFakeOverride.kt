// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt
import kotlin.test.assertEquals

interface Builder<in R> {
    fun onTimeout(timeMillis: Long, block: suspend () -> R) {}
}

open class Impl<R> : Builder<R>

fun box(): String {
    assertEquals("suspend () -> R", Impl<*>::onTimeout.parameters.last().type.toString())
    return "OK"
}
