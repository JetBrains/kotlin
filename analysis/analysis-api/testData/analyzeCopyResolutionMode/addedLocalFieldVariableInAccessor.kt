// COMPILATION_ERRORS
// MODULE: original
val i: Int
    get() {
        field
        return 0
    }

// MODULE: copy
val i: Int
    get() {
        val field = 1
        field
        return 0
    }