// PROBLEM: none
// WITH_RUNTIME
class Test {
    var s: String? = null

    fun test() {
        if (s != null) {
            <caret>requireNotNull(s)
        }
    }
}