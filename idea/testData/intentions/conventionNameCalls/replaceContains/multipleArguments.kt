// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: should not have first parameter with default values. and should have remaining parameters with default value
fun test() {
    class Test{
        operator fun contains(a: Int, b: Int) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(1, 2)
}
