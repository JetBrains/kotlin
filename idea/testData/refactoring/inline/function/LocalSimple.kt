fun bar(s: String) {}

fun foo() {
    fun local() {
        bar("Test")
    }

    // TODO: should be available
    <caret>local()
}