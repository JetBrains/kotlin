// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test


inline fun <T> mrun(lambda: () -> T): T = lambda()


// FILE: 2.kt
import test.*

object Whatever {
    operator fun getValue(thisRef: Any?, prop: Any?) = "OK"
}

fun box(): String {
    val key by Whatever
    return mrun {
        object {
            val keys = key
        }.keys
    }
}
