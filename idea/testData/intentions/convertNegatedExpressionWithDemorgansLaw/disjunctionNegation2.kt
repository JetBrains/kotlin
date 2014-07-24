// INTENTION_TEXT: Replace '||' with '&&'

fun foo(a: Boolean, b: Boolean, c: Boolean) : Boolean {
    return !(<caret>a || (b && c))
}