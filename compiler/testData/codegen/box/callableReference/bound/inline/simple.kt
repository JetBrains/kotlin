// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

inline fun foo(x: () -> String) = x()

fun String.id() = this

fun box() = foo("OK"::id)
