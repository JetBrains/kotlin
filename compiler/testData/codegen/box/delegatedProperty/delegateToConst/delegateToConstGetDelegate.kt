// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

val s: String by 1

fun box() = if (::s.getDelegate() == 1) "OK" else "FAILURE"
