fun test(b: Boolean): (Int) -> Int {
    if (b) {
        fun(n: Int) = n + 1
    }
    else {
        fun(n: Int) = n - 1
    }
}