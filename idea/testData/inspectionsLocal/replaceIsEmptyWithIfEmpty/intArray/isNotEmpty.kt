// PROBLEM: none
// WITH_RUNTIME
fun test(intArr: IntArray): IntArray {
    return if (intArr.isNotEmpty<caret>()) {
        intArr
    } else {
        intArrayOf(1)
    }
}