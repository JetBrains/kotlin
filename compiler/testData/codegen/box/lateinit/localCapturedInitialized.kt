// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    lateinit var s: String

    fun foo() = s

    s = "OK"
    return foo()
}
