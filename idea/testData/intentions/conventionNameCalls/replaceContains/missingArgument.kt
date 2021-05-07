// IS_APPLICABLE: false
// ERROR: No value passed for parameter 'b'
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
// IGNORE_FE10_BINDING_BY_FIR
fun test() {
    class Test{
        operator fun contains(a: Int, b: Int): Boolean = true
    }
    val test = Test()
    test.cont<caret>ains(0)
}
