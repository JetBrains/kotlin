fun foo() {
    fun bar(): String {
        return ""
    }
}

fun main() {
    <caret>foo()
}