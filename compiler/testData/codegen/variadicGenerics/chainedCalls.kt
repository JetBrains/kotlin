// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <vararg Ts, R> variadicOuter(
    vararg args: *Ts,
    transform: (*Ts) -> R
): R = variadicInner(*args, transform = transform)

fun <vararg Ts, R> variadicInner(
    vararg args: *Ts,
    transform: (*Ts) -> R
) = transform(args)

fun box(): String {
    return variadicOuter("O", "K") { o, k -> o + k}
}
