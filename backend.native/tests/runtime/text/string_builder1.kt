package runtime.text.string_builder1

import kotlin.test.*

@Test fun runTest() {
    val a = StringBuilder()
    a.append("Hello").appendln("Kotlin").appendln(42).appendln(0.1).appendln(true)
    println(a.toString())	
}
