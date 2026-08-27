fun f() {
    object A

    fun g() {
        class A {companion object}

        <expr>A</expr>
    }
}

// IGNORE_LOOKUP_LOCALLY
