// IS_APPLICABLE: false

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
            val x = "AA"
            doSomething(x)
        }
        2 -> {
            val x = "BB"
            doSomething(x)
        }
        else -> {
            val x = "CC"
            doSomething(x)
        }
    }
}
