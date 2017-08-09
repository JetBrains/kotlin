impl fun foo(n: Int, action: (Int) -> Int) = action(n)

fun test() {
    foo(1) { n -> n + 1 }
}