// !LANGUAGE: +NewInference

fun <vararg Ts, R> variadic (
    vararg args: *Ts,
    last: String,
    transform: (*Ts, String) -> R
): R {
    return transform(args, last)
}

fun box(): String {
    return variadic("O", 42, last = "K") { o, _, k ->
        o + k
    }
}