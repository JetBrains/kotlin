// IS_APPLICABLE: false
// ERROR: No value passed for parameter b
fun test() {
    class Test{
        operator fun contains(a: Int, b: Int): Boolean = true
    }
    val test = Test()
    test.cont<caret>ains(0)
}
