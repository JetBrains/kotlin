fun test(n: Int): String? {
    var res = <caret>if (n == 1) {
        if (3 > 2) {
            println("***")
            "one"
        } else {
            println("***")
            "???"
        }
    } else if (n == 2) {
        println("***")
        null
    } else {
        println("***")
        "too many"
    }

    return res
}