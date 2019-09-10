// !LANGUAGE: +NewInference +VariadicGenerics
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <vararg Ts, R> variadic(
    vararg args: *Ts,
    transform: (*Ts) -> R
) = transform(args)

fun <vararg Ts, R> wrapper(
    vararg args: *Ts,
    transform: (*Ts) -> R
) = variadic(
    *args,
    transform = { transform(it) }
)

fun box(): String {
    return wrapper(Unit, "O", "K") { _, o, k -> o + k}
}