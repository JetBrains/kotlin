// COMPILATION_ERRORS

fun declaration() {
    [val first, var second,] = x
    [var first] = x
    [val first: String] = x
}

fun loop() {
    for ([val first, val second,] in x) {}
    for ([val first] in x) {}
    for ([val first: String] in x) {}
}

fun lambda() {
    foo { [val first, val second,] -> }
    foo { [val first] -> }
    foo { [val first: String] -> }
}