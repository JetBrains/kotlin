fun foo() {
    <expr>bar(2, l@{ it })</expr>
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}