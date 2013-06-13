fun test(n: Int): String {
    var res<caret> = when (n) {
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