// PROBLEM: none
// WITH_RUNTIME
fun test(arr: Array<String>): Array<String> {
    return if (arr.isEmpty<caret>()) {
        arrayOf("a")
    } else {
        arr
    }
}