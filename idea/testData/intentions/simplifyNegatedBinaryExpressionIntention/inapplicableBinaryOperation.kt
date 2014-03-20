// IS_APPLICABLE: false
fun lt(a: Int, b: Int): Boolean = a < b
fun test(n: Int) {
    !(1<caret> lt 2)
}