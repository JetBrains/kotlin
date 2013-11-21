fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> {
            res = "one"
            val x = "A"
            println(x)
        }
        2 -> {
            res = "two"
            val x = "B"
            println(x)
        }
        else -> {
            res = "unknown"
            val x = "C"
            println(x)
        }
    }

    when (n) {
        1 -> {
            val y = "AA"
            println(y)
        }
        2 -> {
            val y = "BB"
            println(y)
        }
        else -> {
            val y = "CC"
            println(y)
        }
    }
}