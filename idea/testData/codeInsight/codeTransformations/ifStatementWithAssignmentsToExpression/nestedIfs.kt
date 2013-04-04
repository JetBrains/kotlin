fun test(n: Int): String {
    var res: String

    <caret>if (n == 1) {
        if (3 > 2) {
            println("***")
            res = "one"
        } else {
            println("***")
            res = "???"
        }
    } else if (n == 2) {
        println("***")
        res = "two"
    } else {
        println("***")
        res = "too many"
    }

    return res
}