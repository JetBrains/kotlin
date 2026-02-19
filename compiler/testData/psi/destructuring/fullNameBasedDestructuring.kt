// COMPILATION_ERRORS

fun declaration() {
    (val first, var second,) = x
    (var first) = x
    (val first: String) = x
    (val aa = first) = x
    (val aa: String = first) = x
}

fun loop() {
    for ((val first, val second,) in x) {}
    for ((val first) in x) {}
    for ((val first: String) in x) {}
    for ((val aa = first) in x) {}
    for ((val aa: String = first) in x) {}
}

fun lambda() {
    foo { (val first, val second,) -> }
    foo { (val first) -> }
    foo { (val first: String) -> }
    foo { (val aa = first) -> }
    foo { (val aa: String = first) -> }
}