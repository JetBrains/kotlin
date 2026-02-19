// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline fun <reified P> cast(value: Any): P =
    cast0<Int, P>(value)

inline fun <reified P, reified Z> cast0(
    value: Any,
    func: (Any) -> Z = { it as Z }
): Z = func(value)

// FILE: main.kt
fun box(): String =
    cast<String>("OK")
