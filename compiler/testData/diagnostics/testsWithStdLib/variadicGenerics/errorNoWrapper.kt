// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNCHECKED_CAST

class Box<T>(val value: T)

fun <R, vararg Ts> variadic (
    vararg arguments: *Box<Ts>,
    transform: (*Ts) -> R
): Box<R> {
    val args = Tuple<Any?>(arguments.size)
    for (i in 0 until arguments.size) {
        args[i] = (arguments[i] as Box<Any?>).value
    }
    return Box(transform(args as Tuple<Ts>))
}

val something = variadic(<!TYPE_MISMATCH!>"foo"<!>, Box(15)) { _, _ ->

}