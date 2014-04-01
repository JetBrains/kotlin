// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun test(n: Int): String {
    var res: String = ""

    <caret>if (n == 1) {
        doSomething("***")
        res = "one"
    } else {
        var res: String

        doSomething("***")
        res = "two"
    }

    return res
}
