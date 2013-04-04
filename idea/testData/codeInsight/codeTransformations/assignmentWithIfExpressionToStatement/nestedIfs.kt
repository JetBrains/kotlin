fun test(n: Int): String {
    var res: String

    <caret>res = if (n == 1) {
        if (3 > 2) {
            println("***")
            "one"
        } else {
            println("***")
            "???"
        }
    } else if (n == 2) {
        println("***")
        "two"
    } else {
        println("***")
        "too many"
    }

    return res
}