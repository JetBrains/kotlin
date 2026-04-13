fun test() {
    while(true) {
        val x = 45 ?: <expr>continue</expr>
        return
    }
}

// FIR considers all expressions of type `Nothing` as unused.
