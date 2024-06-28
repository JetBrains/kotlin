fun test() {
    block("foo") { s ->
        block(42) { n ->
            <expr>s.length + n</expr>
        }
    }
}

fun <T> block(obj: T, block: (T) -> Unit) {
    block(obj)
}