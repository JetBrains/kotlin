// IS_APPLICABLE: false
fun test(n: Int): String {
    var res: String = ""

    <caret>if (n == 1) {
        println("***")
        res = "one"
    } else {
        var res: String

        println("***")
        res = "two"
    }

    return res
}