// IS_APPLICABLE: false
fun Int.lt(b: Int): Boolean = this < b
fun test(n: Int) {
    <caret>!(1 lt 2)
}
