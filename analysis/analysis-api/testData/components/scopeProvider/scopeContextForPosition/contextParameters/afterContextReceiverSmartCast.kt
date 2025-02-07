
context(Int)
fun bar() {

}

fun bar() {

}

context(Any)
fun foo(p: Boolean) {
    if (this@Any is Int) {
        bar()
    }

    <expr>bar()</expr>
}

// LANGUAGE: +ContextReceivers
