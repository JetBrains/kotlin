// IGNORE_BACKEND: JS_IR
interface Named {
    val name: String
}

enum class E : Named {
    OK
}

fun box(): String {
    return E.OK.name
}
