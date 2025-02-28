fun test(x: Int, y: Int) {
    if (x < (if (y > 115) 1 else 2)) {}
}

// COMPILATION_ERRORS
