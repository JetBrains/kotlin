// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <vararg Ts, R> variadic (
    first: String,
    vararg args: *Ts,
    last: String,
    transform: (String, *Ts, String) -> R
): R {
    return transform(first, args, last)
}

fun box(): String {
    return variadic("O", 1, "K", 3, last = "") { o, one, k, three, empty ->
        o + k
    }
}