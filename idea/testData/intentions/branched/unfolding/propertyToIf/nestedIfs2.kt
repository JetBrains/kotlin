fun <T> doSomething(a: T) {}

fun test(n: Int): String? {
    var res = <caret>if (n == 1) {
        if (3 > 2) {
            doSomething("***")
            "one"
        } else {
            doSomething("***")
            "???"
        }
    } else if (n == 2) {
        doSomething("***")
        null
    } else {
        doSomething("***")
        "too many"
    }

    return res
}
