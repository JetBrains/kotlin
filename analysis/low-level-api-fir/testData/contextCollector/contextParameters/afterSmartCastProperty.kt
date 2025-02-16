
context(_: Int)
fun bar() {

}

fun bar() {

}

context(c: Any)
val foo: Unit
    get() {
        if (c is Int) {
            bar()
        }

        <expr>bar()</expr>
}
// LANGUAGE: +ContextParameters
