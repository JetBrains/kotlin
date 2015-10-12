// IS_APPLICABLE: false
fun Any.foo(<caret>s: String, n: Int): Boolean {
    return s.length - n/2 > 1
}

fun test() {
    "0".foo("1", 2)
}