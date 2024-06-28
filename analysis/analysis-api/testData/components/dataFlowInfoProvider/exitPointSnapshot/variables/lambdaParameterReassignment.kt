fun test() {
    block { n ->
        <expr>n = 1</expr>
    }
}

fun block(block: (Int) -> Unit) {}