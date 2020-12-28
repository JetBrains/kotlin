class A {
    var v: String? = null
}

fun foo(): String? {
    val t = A().v

    return t
}