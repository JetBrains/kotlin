class My(val x: Int)

fun foo(arg: Any?): Int? {
    return if (<caret>arg is My) arg.x.hashCode() else null
}