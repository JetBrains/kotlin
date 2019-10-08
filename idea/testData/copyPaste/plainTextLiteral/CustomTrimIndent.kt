fun String.trimIndent() = this

fun test() = doTest("""
    <caret>
""".trimIndent())

fun doTest(rubyCode: String) {
    // some code here
}

