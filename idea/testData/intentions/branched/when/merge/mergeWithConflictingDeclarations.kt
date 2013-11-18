// IS_APPLICABLE: false
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
            val x = "AA"
            println(x)
        }
        2 -> {
            val x = "BB"
            println(x)
        }
        else -> {
            val x = "CC"
            println(x)
        }
    }
}