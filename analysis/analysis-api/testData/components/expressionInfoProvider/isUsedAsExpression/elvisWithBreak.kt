fun test() {
    while(true) {
        val x = 45 ?: <expr>break</expr>
        return
    }
}

// FIR considers all expressions of type `Nothing` as unused.
