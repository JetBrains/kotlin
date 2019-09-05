// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <vararg Ts, R> variadic(
    vararg args: *Ts,
    transform: (*Ts) -> R
) = transform(args)

fun <T1, T2, R> simple(
    t1: T1,
    t2: T2,
    transform: (T1, T2) -> R
): R = variadic(t1, t2) { arg1, arg2 ->
    transform(arg1, arg2)
}

fun box(): String = simple("O", "K") { o, k -> o + k }
