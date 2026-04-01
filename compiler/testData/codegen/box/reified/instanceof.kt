// WITH_STDLIB

// FILE: lib.kt
inline fun<reified T> isinstance(x: Any?): Boolean {
    return x is T
}

// FILE: main.kt
fun box(): String {
    require(isinstance<String>("abc"))
    require(isinstance<Int>(1))
    require(!isinstance<Int>("abc"))

    return "OK"
}
