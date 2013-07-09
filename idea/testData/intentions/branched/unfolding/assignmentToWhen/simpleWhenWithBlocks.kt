fun test(n: Int): String {
    var res: String

    <caret>res = when (n) {
        1 -> {
            println("***")
            "one"
        }
        else -> {
            println("***")
            "two"
        }
    }

    return res
}