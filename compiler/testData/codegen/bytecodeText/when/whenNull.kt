fun test(a: Any?, b: Any?, c: Any?) {
    when (null) {
        a -> throw IllegalArgumentException("a is null")
        b -> throw IllegalArgumentException("b is null")
        c -> throw IllegalArgumentException("c is null")
    }
}

// 0 areEqual
// 3 IFNONNULL