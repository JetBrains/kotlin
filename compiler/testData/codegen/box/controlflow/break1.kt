// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    loop@ while (true) {
        sb.appendLine("Body")
        break
    }
    sb.appendLine("Done")

    assertEquals("Body\nDone\n", sb.toString())
    return "OK"
}
