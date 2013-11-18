fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> {
            res = "one"
            println("A")
        }
        2 -> {
            res = "two"
            println("B")
        }
        else -> {
            res = "unknown"
            println("C")
        }
    }

    when (n) {
        1 -> {
            println("A")
            println("AA")
        }
        2 -> {
            println("B")
            println("BB")
        }
        else -> {
            println("C")
            println("CC")
        }
    }
}