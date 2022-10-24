fun test() {
    val x = 45 ?: <expr>return</expr>
    return
}

// IGNORE_FE10
// FIR considers all expressions of type `Nothing` as unused.