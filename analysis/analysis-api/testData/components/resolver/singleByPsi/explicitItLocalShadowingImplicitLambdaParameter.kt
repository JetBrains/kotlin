fun f(func: (Int) -> Unit) {
    func(1)
}

fun f2() {
    f {
        val it = 0
        <expr>it</expr> + 1
    }
}

// ISSUE: KT-89166
