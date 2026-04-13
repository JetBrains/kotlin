fun test() {
    val x = 45 ?: <expr>return</expr>
    return
}

// FIR considers all expressions of type `Nothing` as unused.
