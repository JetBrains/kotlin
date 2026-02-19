
context(_: Int)
fun bar() {

}

fun bar() {

}

context(c: Any)
fun foo(p: Boolean) {
    if (c is Int) {
        <expr>bar()</expr>
    }

    bar()
}
// LANGUAGE: +ContextParameters
