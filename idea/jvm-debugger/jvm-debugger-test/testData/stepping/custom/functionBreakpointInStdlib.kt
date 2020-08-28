package test

import java.io.StringReader

fun main() {
    val reader = StringReader("test")
    reader.readText()
}

// ADDITIONAL_BREAKPOINT: ReadWrite.kt / public fun Reader.readText() / fun
// STEP_OVER: 1