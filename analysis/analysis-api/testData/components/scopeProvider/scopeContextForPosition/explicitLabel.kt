fun test() {
    block("foo") foo@ {
        <expr>length</expr>
    }
}

fun block(value: String, block: String.() -> Unit) {
    value.block()
}
