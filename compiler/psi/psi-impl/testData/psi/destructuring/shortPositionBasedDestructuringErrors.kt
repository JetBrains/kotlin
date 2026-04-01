// COMPILATION_ERRORS

fun declaration() {
    if (1 == 1) { val [first, b = second] = x }
    if (1 == 2) { var [a = first] = x }
    if (1 == 3) { val [a: String = first] = x }
    if (1 == 4) { val [first) = x }
    if (1 == 5) { val (first] = x }
}

fun loop() {
    for ([first, b = second,] in x) {}
    for ([a = first] in x) {}
    for ([a: String = first] in x) {}
    for ([first) in x) {}
    for ((first] in x) {}
}

fun lambda() {
    foo { [first, b = second,] -> }
    foo { [a = first] -> }
    foo { [a: String = first] -> }
    foo { [first) -> }
    foo { (first] -> }
}