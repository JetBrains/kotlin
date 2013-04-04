fun test(n: Int): String {
    var res: String

    <caret>res = if (n == 1) {
        println("***")
        "one"
    } else {
        println("***")
        "two"
    }

    return res
}