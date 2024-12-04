inline fun <reified P> cast(value: Any): P =
    cast0<Int, P>(value)

inline fun <reified P, reified Z> cast0(
    value: Any,
    func: (Any) -> Z = { it as Z }
): Z = func(value)

fun box(): String =
    cast<String>("OK")
