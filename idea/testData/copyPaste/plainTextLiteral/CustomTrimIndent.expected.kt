fun String.trimIndent() = this

fun test() = doTest("""
    def foo(a)
        a ? 0 : 1
    end
""".trimIndent())

fun doTest(rubyCode: String) {
    // some code here
}
