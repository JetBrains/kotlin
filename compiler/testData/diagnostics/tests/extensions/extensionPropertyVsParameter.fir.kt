val Int.foo: Int
    get() = this


fun test(foo: Int) {
    test(4.foo)
    test(foo)
}