// HIGHLIGHT: INFORMATION
class My(val x: Int)

fun foo(arg: Any?): Int? {
    return i<caret>f (arg is My) arg.x.hashCode() else null
}