// IS_APPLICABLE: false

fun foo(arg: Any?): Any? {
    return if (<caret>arg != null) arg else null
}