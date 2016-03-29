// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have at least 1 value parameter
fun test() {
    class Test{
        operator fun get() : Int = 0
    }
    val test = Test()
    test.g<caret>et()
}
