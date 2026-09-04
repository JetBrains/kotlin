fun f(func: (Int) -> Unit) {
    func(1)
}

fun f2() {
    val it = 0
    f {
        <expr>it</expr> + 1
    }
}

// IGNORE_LOOKUP_LOCALLY
// ISSUE: KT-89166
