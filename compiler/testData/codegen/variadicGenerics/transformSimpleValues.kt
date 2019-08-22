// !LANGUAGE: +NewInference
// WITH_RUNTIME

fun box(): String {
    return getOK()
}

fun getOK() = varargLambda(42, "O", "K", Unit) { arg1, arg2, arg3, arg4 ->
    arg2 + arg3
}

fun <vararg Ts, R> varargLambda(
    vararg arguments: *Ts,
    block: (*Ts) -> R
): R {
    return block(arguments)
}