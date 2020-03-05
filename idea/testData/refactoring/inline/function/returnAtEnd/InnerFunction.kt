fun foo() {
    object {
        fun bar(): String {
            return ""
        }
    }
}

fun main() {
    <caret>foo()
}