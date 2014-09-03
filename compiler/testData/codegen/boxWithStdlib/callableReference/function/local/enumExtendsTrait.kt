trait Named {
    fun name(): String
}

enum class E : Named {
    OK
}

fun box(): String {
    return E.OK.(Named::name)()
}
