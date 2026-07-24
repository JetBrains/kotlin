fun test() {
    block("foo") foo@ {
        block("bar") bar@ {
            <expr>length</expr>
        }
    }
}

fun block(value: String, block: String.() -> Unit) {
    value.block()
}
