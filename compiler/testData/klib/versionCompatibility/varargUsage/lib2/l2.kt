fun useFoo(): String {
    foo1(1)
    foo2(1)
    foo1(1, 2)
    foo2(1, 2)
    foo1(params = intArrayOf(1, 2))
    foo2(params = intArrayOf(1, 2))
    return "OK"
}
