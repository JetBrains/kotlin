fun <T> doSomething(a: T) {}

fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> {
            res = "one"
            val x = "A"
            doSomething(x)
        }
        2 -> {
            res = "two"
            val x = "B"
            doSomething(x)
        }
        else -> {
            res = "unknown"
            val x = "C"
            doSomething(x)
        }
    }

    when (n) {
        1 -> {
            val y = "AA"
            doSomething(y)
        }
        2 -> {
            val y = "BB"
            doSomething(y)
        }
        else -> {
            val y = "CC"
            doSomething(y)
        }
    }
}
