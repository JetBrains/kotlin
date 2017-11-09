class My(val x: Int)

fun foo(arg: My?): Int {
    return if (<caret>arg != null) arg.x else 42
}