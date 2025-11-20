// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()


enum class Zzz {
    Z {
        init {
            sb.appendLine(this.name)
        }
    }
}

fun box(): String {
    sb.appendLine(Zzz.Z)
    assertEquals("""
        Z
        Z

    """.trimIndent(), sb.toString())
    return "OK"
}
