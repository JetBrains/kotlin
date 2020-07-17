inline class X(val x: Any?)

fun useX(x: X): String = x.x as String

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X("OK") })