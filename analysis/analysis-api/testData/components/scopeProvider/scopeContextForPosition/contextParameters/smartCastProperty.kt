
context(_: Int)
fun bar() {

}

fun bar() {

}

context(c: Any)
val foo: Unit
    get() {
        if (c is Int) {
            <expr>bar()</expr>
        }

        bar()
}
// LANGUAGE: +ContextParameters
