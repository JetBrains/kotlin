// IS_APPLICABLE: true
// WITH_RUNTIME

class Test {
    fun test() {
        with(Any()) {
            val f = { s: String<caret> -> foo(s) }
        }
    }
    fun foo(s: String) {}
}