fun test() {
    [tailRecursive] fun g3(counter : Int, x : Any) {
        if (counter > 0) { g3(counter - 1, "tail") }
    }
    g3(1000000, "test")
}

fun box() : String {
    test()
    return "OK"
}