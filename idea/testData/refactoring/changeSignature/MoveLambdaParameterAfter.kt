fun foo(x: Int, cl: () -> Int, y: Int): Int {
    return x + cl()
}

fun bar() {
    foo(
            1,
            {
                3
            },
            2
    )
}