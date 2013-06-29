// IS_APPLICABLE: false
fun test(n: Int): String {
    var res: String = ""
    var res2: String = ""

    <caret>when (n) {
        1 -> {
            println("***")
            res = "one"
        }
        else -> {
            println("***")
            res2 = "two"
        }
    }

    return res + res2
}