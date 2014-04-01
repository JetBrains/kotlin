// SHOULD_FAIL_WITH: duplicate.or.missing.arguments
// ERROR: No value passed for parameter b
fun test() {
    class Test{
        fun contains(a: Int, b: Int): Boolean = true
    }
    val test = Test()
    test.cont<caret>ains(0)
}
