// WITH_STDLIB

inline fun<reified T> isinstance(x: Any?): Boolean {
    return x is T
}

fun box(): String {
    require(isinstance<String>("abc"))
    require(isinstance<Int>(1))
    require(!isinstance<Int>("abc"))

    return "OK"
}
