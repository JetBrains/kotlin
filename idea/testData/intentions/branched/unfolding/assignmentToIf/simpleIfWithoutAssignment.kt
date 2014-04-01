// IS_APPLICABLE: false
fun test(n: Int): String {
    var res: String = ""

    <caret>if (n == 1) {
        "one"
    } else {
        "two"
    }

    return res
}
