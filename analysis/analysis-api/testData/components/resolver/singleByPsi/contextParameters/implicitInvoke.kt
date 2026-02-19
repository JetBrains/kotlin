class A

context(a: A)
operator fun A.invoke() {}

fun usage() {
    with(A()) {
        var thisProp = this
        <expr>thisProp()</expr>
    }
}

// LANGUAGE: +ContextParameters
