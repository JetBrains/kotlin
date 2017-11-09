fun test() {
    var res: String? = null
    var foo: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        res = "failure"
    } finally {
        foo = "finally"
    }
}