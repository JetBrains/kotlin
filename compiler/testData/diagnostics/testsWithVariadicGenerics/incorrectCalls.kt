// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER

fun <R, vararg Ts> variadic (
    vararg arguments: *Ts,
    transform: (*Ts) -> R
): R {
    return transform(arguments)
}

val v1 = variadic<!UNSUPPORTED!><Unit, Int, String><!>(42, "foo") { first, second ->

}