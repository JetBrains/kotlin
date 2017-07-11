fun doSomething() {}

fun test() {
    var res: String? = null

    <caret>try {
        doSomething()
        res = "success"
    } catch (e: Exception) {
        doSomething()
        res = "failure"
    }
}
