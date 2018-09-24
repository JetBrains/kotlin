fun foo(arg: Any?): Any? {
    return <caret>if (arg != null) arg else null
}