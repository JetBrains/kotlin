// WITH_RUNTIME
class Test {
    fun function(s: String): Boolean = true
    fun test() {
        "".let {<caret> this::function }
    }
}
