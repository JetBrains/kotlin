// SHOULD_FAIL_WITH: invalid.arguments
fun test() {
    class Test{
        fun get(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.g<caret>et(c=3)
}
