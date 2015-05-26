// WITH_RUNTIME
val String.<caret>foo: String
    get() = if (isEmpty()) "" else substring(1).foo