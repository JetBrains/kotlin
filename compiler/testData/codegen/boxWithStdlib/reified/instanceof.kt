inline fun<reified T> isinstance(x: Any?): Boolean {
    return x is T
}

fun box(): String {
    assert(isinstance<String>("abc"))
    assert(isinstance<Int>(1))
    assert(!isinstance<Int>("abc"))

    return "OK"
}
