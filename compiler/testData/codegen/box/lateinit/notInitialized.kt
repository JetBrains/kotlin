// WITH_STDLIB

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

val sb = StringBuilder()

fun box(): String {
    val a = A()
    try {
        sb.appendLine(a.foo())
    }
    catch (e: RuntimeException) {
        sb.append("OK")
        return sb.toString()
    }
    return "Fail"
}
