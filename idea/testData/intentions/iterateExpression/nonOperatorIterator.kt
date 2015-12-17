// WITH_RUNTIME
// IS_APPLICABLE: false
class T<U> {
    fun <U> T<U>.iterator(): Iterator<U> = listOf<U>().iterator()
}

fun test() {
    T<Int>()<caret>
}