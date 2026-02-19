// COMPILATION_ERRORS

fun declaration() {
    val (val first, var second,) = x;
    val (first, val second) = x;
    (first, second) = x;
    (val first: String, second) = x;
}

fun loop() {
    for ((first, val second) in x) {}
    for ((first, var second) in x) {}
    for ((val first, second) in x) {}
    for ((var first) in x) {}
    for ((val first, var second) in x) {}
}

fun lambda() {
    foo { (first, val second) -> }
    foo { (first, var second) -> }
    foo { (val first, second) -> }
    foo { (var first) -> }
    foo { (val first, var second) -> }
}