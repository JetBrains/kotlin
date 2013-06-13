// IS_APPLICABLE: false
fun test(n: Int): String {
    var res: String = ""

    <caret>when (n) {
        1 -> {
            println("***")
            res = "one"
        }
        else -> {
            var res: String

            res = "two"
            println("***")
        }
    }

    return res
}