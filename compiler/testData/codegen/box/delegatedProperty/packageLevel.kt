// WITH_STDLIB

import kotlin.test.*

import kotlin.reflect.KProperty

val sb = StringBuilder()

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        sb.appendLine(p.name)
        return 42
    }
}

val x: Int by Delegate()

fun box(): String {
    sb.appendLine(x)

    assertEquals("""
        x
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
