private interface Bindings {
    val value: String
}

private fun <T : Any> invokeIt(any: Any, extract: (T) -> String): String {
    @Suppress("UNCHECKED_CAST")
    return extract(any as T)
}

fun typeParameterLambdaArgument(): String? {
    var caught: String? = null
    try {
        invokeIt<Bindings>("wrong-type") { it.value }
    } catch (t: Throwable) {
        caught = t::class.simpleName
    }
    return caught
}

fun box(): String {
    if (typeParameterLambdaArgument() == "ClassCastException")
        return "OK"
    return "BAD"
}
