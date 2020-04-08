// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR

inline class X(val x: Any)

fun useX(x: X): String = x.x as String

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call(fun(): X { return X("OK") }))