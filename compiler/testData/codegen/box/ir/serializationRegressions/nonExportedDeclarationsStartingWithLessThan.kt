// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB

// A leading `<` in a function name works as long as the function is not visible to other modules,
// i.e. it is private or internal, even if other modules use it indirectly through public declarations.

// MODULE: lib
// FILE: lib.kt
package lib

class RenderingTest {
    private fun `<html> rendering`(): String = "O"
    internal fun `<body> rendering`(): String = "K"

    fun render(): String = `<html> rendering`() + `<body> rendering`()
}

private fun `<html> rendering`(): String = "O"
internal fun `<body> rendering`(): String = "K"

fun render(): String = `<html> rendering`() + `<body> rendering`()

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun box(): String {
    if (render() != "OK") return "FAIL: top-level functions"
    if (RenderingTest().render() != "OK") return "FAIL: member functions"
    return "OK"
}
