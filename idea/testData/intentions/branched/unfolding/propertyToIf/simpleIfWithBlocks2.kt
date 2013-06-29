fun test(n: Int): String {
    var res<caret> = if (n == 1) {
        println("***")
        "one"
    } else {
        println("***")
        "two"
    }

    return res
}