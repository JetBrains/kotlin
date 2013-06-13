fun test(n: Int): String {
    val res<caret> = if (n == 1) {
        println("***")
        "one"
    } else {
        println("***")
        "two"
    }

    return res
}