fun test() {
    block("foo") foo@ {
        block("foo2") foo@ {
            <expr>length</expr>
        }
    }
}

fun block(value: String, block: String.() -> Unit) {
    value.block()
}
