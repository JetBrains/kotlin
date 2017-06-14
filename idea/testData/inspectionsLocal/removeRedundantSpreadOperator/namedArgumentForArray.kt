fun foo(vararg x: String) {}

fun bar() {
    foo(*arrayOf<caret>(elements = "abc"))
}
