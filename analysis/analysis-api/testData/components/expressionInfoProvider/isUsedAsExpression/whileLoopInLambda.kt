fun test(text: String) {
    call {
        <expr>while(true) {

        }</expr>
    }
}

fun call(block: () -> Unit) = block()