// PROBLEM: none
// ERROR: Type mismatch: inferred type is Long? but Long was expected
fun test(b: Boolean, x: Long, y: Long?) {
    var num: Long = 0L
    <caret>if (b) {
        num += x
    } else {
        num += y
    }
}