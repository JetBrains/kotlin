// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: should not have varargs
fun test() {
    class Test{
        operator fun contains(vararg b: Int, c: Int = 0): Boolean = true
    }
    val test = Test()
    test.contains<caret>(0, 1)
}
