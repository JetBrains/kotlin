fun doSomething() {}

fun test(n: Int) {
    var res: String? = null
    var foo: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        throw e
    } finally {
        if (n == 1)
            doSomething()
        else
            foo = "finally"
    }
}