// IS_APPLICABLE: false
fun Int.lt(b: Int): Boolean = this < b
fun test(n: Int) {
    !(1<caret> lt 2)
}
