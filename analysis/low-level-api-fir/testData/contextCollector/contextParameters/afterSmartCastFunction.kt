
context(_: Int)
fun bar() {

}

fun bar() {

}

context(c: Any)
fun foo(p: Boolean) {
    if (c is Int) {
        bar()
    }

    <expr>bar()</expr>
}

// LANGUAGE: +ContextParameters
