
context(Int)
fun bar() {

}

fun bar() {

}

context(Any)
fun foo(p: Boolean) {
    if (this@Any is Int) {
        <expr>bar()</expr>
    }

    bar()
}

// LANGUAGE: +ContextReceivers
