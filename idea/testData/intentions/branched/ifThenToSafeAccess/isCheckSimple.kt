class My(val x: Int)

fun foo(arg: Any?): My? {
    return if (<caret>arg is My) arg else null
}