// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun foo(s: String = "", vararg args: Any) {
    if (args == null) {
        sb.appendLine("Failed!")
    } else {
        sb.append("$s ")
        args.forEach {
            sb.append("$it")
        }
        sb.appendLine(", Correct!")
    }
}

fun box(): String {
    foo("Hello")
    foo("Hello", "World")
    foo()

    assertEquals("""
        Hello , Correct!
        Hello World, Correct!
         , Correct!

    """.trimIndent(), sb.toString())
    return "OK"
}
