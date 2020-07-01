// PROBLEM: none
// WITH_RUNTIME
fun test(arr: Array<String>): Array<String> {
    return if (arr.isNotEmpty<caret>()) {
        arr
    } else {
        arrayOf("a")
    }
}