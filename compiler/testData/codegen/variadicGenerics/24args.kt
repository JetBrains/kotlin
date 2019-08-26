// !LANGUAGE: +NewInference

fun <R, vararg Ts> variadic (
    vararg arguments: *Ts,
    transform: (*Ts) -> R
): R {
    return transform(arguments)
}

val v1 = variadic("O", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, "K") {
        a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23 ->
    a0 + a23
}

fun box(): String {
    return v1
}
