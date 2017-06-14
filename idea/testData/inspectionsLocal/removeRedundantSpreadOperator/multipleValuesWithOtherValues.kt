fun foo(vararg x: String) {}

fun bar() {
    foo(*arrayOf<caret>("abc", "def"), "ghi", "jkl")
}
