fun test(n: Int): String {
    var res: String

    if (3 > 2) {
        <caret>when(n) {
            1 -> {
                println("***")
                res = "one"
            }
            else -> {
                println("***")
                res = "two"
            }
        }
    } else {
        println("***")
        res = "???"
    }

    return res
}