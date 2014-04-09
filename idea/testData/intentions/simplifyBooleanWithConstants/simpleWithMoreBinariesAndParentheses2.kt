fun foo(y: Boolean) {
    bar() && y || <caret>(y && true && bar()) || false
}

fun bar(): Boolean = false