// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have a single value parameter
fun test() {
    class Test{
        operator fun contains(a: Int=1, b: Int=2) : Boolean = true
    }
    val test = Test()
    test.contai<caret>ns(b=3)
}
