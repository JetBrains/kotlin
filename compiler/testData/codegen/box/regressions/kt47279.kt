fun test() {
    null?.run { return }
}

fun box(): String {
    test()
    return "OK"
}
