// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
// IGNORE_FE10_BINDING_BY_FIR
fun test() {
    class Test{
        operator fun contains(vararg b: Int, c: Int = 0): Boolean = true
    }
    val test = Test()
    test.contains<caret>(c=5)
}
