// INTENTION_TEXT: Replace call with binary operator
// IS_APPLICABLE: false

fun test() {
    class Test {
        operator fun mod(a: Int): Test = Test()
    }
    val test = Test()
    test.mod<caret>(1)
}
