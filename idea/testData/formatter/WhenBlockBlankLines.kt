fun f(x: Int) = when(x) {
    1 -> "Foo"
    2 -> "Bar"
    3 -> {
        "Foo"
    }
    4 -> "Bar"
    else -> "Xyzzy"
}

// SET_INT: BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES = 1
