// WITH_STDLIB

import kotlin.test.*
import kotlin.reflect.KProperty

val sb = StringBuilder()

fun foo(): Int {
   class Delegate {
        operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
            sb.appendLine(p.name)
            return 42
        }
    }

    val x: Int by Delegate()

    return x
}

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        x
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
