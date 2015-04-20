fun test(b: Boolean): (Int) -> Int {
    if (b) {
        fun foo(n: Int) = n + 1
    }
    else {
        fun bar(n: Int) = n - 1
    }
}