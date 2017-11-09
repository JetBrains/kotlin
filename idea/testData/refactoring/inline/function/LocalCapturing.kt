fun bar(s: String) {}

fun foo() {
    val t = "Test"

    fun local() {
        bar(t)
    }

    <caret>local()
}