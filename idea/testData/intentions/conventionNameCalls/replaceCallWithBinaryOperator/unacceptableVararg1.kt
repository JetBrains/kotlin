// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
fun test() {
    class Test{
        operator fun plus(vararg b: Int, c: Int = 0): Int = 0
    }
    val test = Test()
    test.plus<caret>(c=5)
}
