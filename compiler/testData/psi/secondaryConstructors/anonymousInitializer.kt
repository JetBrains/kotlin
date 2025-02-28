class A {
    init {}

    private init {}

    val x = f()
    init {
        x = 1
    }
}

// COMPILATION_ERRORS
