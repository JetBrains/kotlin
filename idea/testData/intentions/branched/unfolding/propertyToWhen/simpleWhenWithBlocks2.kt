fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res<caret> = when (n) {
        1 -> {
            doSomething("***")
            "one"
        }
        else -> {
            doSomething("***")
            "two"
        }
    }

    return res
}
