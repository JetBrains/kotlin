// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <R, vararg Ts> variadicFn(
    vararg args: *Ts,
    transform: (*Ts) -> R
): R {
    return transform(args)
}

fun test() {
    variadicFn("foo", "bar", 14) { first, second, third ->
        first.substring(1)
        second.substring(1)
        third + 5
    }
}