// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
fun test() {
    class Test{
        operator fun contains(a: Int, b: Int) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(1, 2)
}
