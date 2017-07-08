// PROBLEM: none
fun doSomething() {}

fun test() {
    var res: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        res = "failure"
        doSomething()
    }
}