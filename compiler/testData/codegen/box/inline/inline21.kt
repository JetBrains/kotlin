// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

inline fun foo2(block2: () -> Int) : Int {
    sb.appendLine("foo2")
    return block2()
}

inline fun foo1(block1: () -> Int) : Int {
    sb.appendLine("foo1")
    return foo2(block1)
}

fun bar(block: () -> Int) : Int {
    sb.appendLine("bar")
    return foo1(block)
}

fun box(): String {
    sb.appendLine(bar { 33 })

    assertEquals("""
        bar
        foo1
        foo2
        33

    """.trimIndent(), sb.toString())
    return "OK"
}
