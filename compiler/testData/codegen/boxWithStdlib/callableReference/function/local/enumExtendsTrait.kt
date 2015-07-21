interface Named {
    fun name(): String
}

enum class E : Named {
    OK
}

fun box(): String {
    return (Named::name)(E.OK)
}
