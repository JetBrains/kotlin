// PROBLEM: none
fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String = ""

    <caret>if (n == 1) {
        doSomething("***")
        res = "one"
    }

    return res
}
