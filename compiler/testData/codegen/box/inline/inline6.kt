// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()

@Suppress("NOTHING_TO_INLINE")
inline fun foo(body: () -> Unit) {
    sb.appendLine("hello1")
    body()
    sb.appendLine("hello4")
}

// FILE: main.kt
import kotlin.test.*

fun bar() {
    foo {
        sb.appendLine("hello2")
        sb.appendLine("hello3")
    }
}

fun box(): String {
    bar()

    assertEquals("""
        hello1
        hello2
        hello3
        hello4

    """.trimIndent(), sb.toString())
    return "OK"
}
