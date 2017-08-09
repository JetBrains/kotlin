header fun foo(n: Int, action: (Int) -> Int): Int

fun test() {
    foo(1) { n -> n + 1 }
}