// IS_APPLICABLE: false
fun test(n: Int): String {
    var res: String

    <caret>if (n == 1) {
        res = "one"
        println("***")
    } else {
        println("***")
        res = "two"
    }

    return res
}