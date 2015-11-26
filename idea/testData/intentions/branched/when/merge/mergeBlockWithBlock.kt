fun <T> doSomething(a: T) {}

fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> {
            res = "one"
            doSomething("A")
        }
        2 -> {
            res = "two"
            doSomething("B")
        }
        else -> {
            res = "unknown"
            doSomething("C")
        }
    }

    when (n) {
        1 -> {
            doSomething("A")
            doSomething("AA")
        }
        2 -> {
            doSomething("B")
            doSomething("BB")
        }
        else -> {
            doSomething("C")
            doSomething("CC")
        }
    }
}
