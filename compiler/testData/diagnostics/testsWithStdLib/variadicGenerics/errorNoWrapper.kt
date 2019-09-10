// !LANGUAGE: +NewInference +VariadicGenerics
// !DIAGNOSTICS: -UNCHECKED_CAST

class Box<T>(val value: T)

fun <R, vararg Ts> variadic (
    vararg arguments: *Box<Ts>,
    transform: (*Ts) -> R
): Box<R> {
    val args = Tuple<Any?>(arguments.size)
    for (i in 0 until arguments.size) {
        args[i] = arguments.get<Box<Any?>>(i).value
    }
    return Box(transform(args as Tuple<Ts>))
}

val something = variadic(<!TYPE_MISMATCH!>"foo"<!>, Box(15)) { _, _ ->

}