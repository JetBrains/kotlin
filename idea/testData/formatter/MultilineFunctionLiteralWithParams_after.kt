fun test(some: (Int) -> Int) {
}

fun foo() = test() { a ->
    if (true) {
        a
    } else {
        1
    }
}