// COMPILATION_ERRORS

fun declaration() {
    val [first, second,] = x
    var [first] = x
    val [first: String] = x
}

fun loop() {
    for ([first, second,] in x) {}
    for ([first] in x) {}
    for ([first: String] in x) {}
}

fun lambda() {
    foo { [first, second,] -> }
    foo { [first] -> }
    foo { [first: String] -> }
}