inline class X(val x: String?)

fun useX(x: X): String = x.x ?: "fail: $x"

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X("OK") })