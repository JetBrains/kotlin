fun bar(s: String) {}

fun foo() {
    val t = "Test"

    fun local() {
        bar(t)
    }

    // TODO: should be available
    <caret>local()
}