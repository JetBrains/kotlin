// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
fun test() {
    class Test {
        operator fun plus(a: Int, b: Int): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(1, 2)
}
