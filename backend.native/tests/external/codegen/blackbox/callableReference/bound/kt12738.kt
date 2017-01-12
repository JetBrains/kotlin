// Enable when callable references to builtin members is supported
// IGNORE_BACKEND: JS
fun <T> get(t: T): () -> String {
    return t::toString
}

fun box(): String {
    if (get(null).invoke() != "null") return "Fail null"

    return get("OK").invoke()
}
