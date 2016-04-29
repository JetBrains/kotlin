inline fun foo(x: () -> String) = x()

fun String.id() = this

fun box() = foo("OK"::id)
