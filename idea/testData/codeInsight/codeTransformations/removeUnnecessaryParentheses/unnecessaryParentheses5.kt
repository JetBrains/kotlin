fun foo(x: Boolean) : Boolean {
    return x || (x || x<caret>)
}