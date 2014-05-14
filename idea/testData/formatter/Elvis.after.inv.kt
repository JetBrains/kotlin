fun test(a: Int?) {
    a ?: 42

    a ?: 42

    a ?:
            42

    a
            ?: 42

    val some = a ?:
            b ?:
            12
}

// SET_TRUE: ALIGN_MULTILINE_BINARY_OPERATION