// COMPILATION_ERRORS

fun declaration() {
    if (1 == 1) { val [val first, var second,] = x; }
    if (1 == 2) { val [first, val second] = x; }
    if (1 == 3) { [first, second] = x; }
    if (1 == 4) { [val first: String, second] = x; }
    if (1 == 5) { [val a: String = first] = x; }
    if (1 == 6) { [val a) = x }
    if (1 == 7) { (val a] = x }
}

fun loop() {
    for ([first, val second] in x) {}
    for ([first, var second] in x) {}
    for ([val first, second] in x) {}
    for ([var first) in x] {}
    for ([val first, var second] in x) {}
    for ([val a = first] in x) {}
    for ([val first) in x) {}
    for ((val first] in x) {}
}

fun lambda() {
    foo { [first, val second] -> }
    foo { [first, var second] -> }
    foo { [val first, second] -> }
    foo { [var first] -> }
    foo { [val first, var second] -> }
    foo { [val a = first] -> }
    foo { [val first) -> }
    foo { (val first] -> }
}