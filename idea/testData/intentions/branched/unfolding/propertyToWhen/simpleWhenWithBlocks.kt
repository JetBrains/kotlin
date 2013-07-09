fun test(n: Int): String {
    val res<caret> = when (n) {
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