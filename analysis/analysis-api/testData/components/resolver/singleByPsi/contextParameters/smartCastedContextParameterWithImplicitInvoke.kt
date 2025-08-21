class A

context(_: A)
operator fun String.invoke() {}

context(p: T)
fun <T> usage() {
    if (p is A) {
        val s = "str"
        <expr>s()</expr>
        Unit
    }
}

// LANGUAGE: +ContextParameters
