fun test() {
    val x = 45 ?: <expr>throw Exception()</expr>
    return
}

// FIR considers all expressions of type `Nothing` as unused.
