// !LANGUAGE: +NewInference
// WITH_RUNTIME

class Box<T>(val value: T)

fun <T, R, vararg Ts> Box<T>.withOthers (
    vararg others: *Box<Ts>,
    transform: (T, *Ts) -> R
): Box<R> {
    val args = Tuple<Any?>(others.size)
    for (i in 0 until others.size) {
        args[i] = (others[i] as Box<Any?>).value
    }
    return Box(transform(this.value, args as Tuple<Ts>))
}

fun box(): String {
    return Box("O").withOthers(Box(15), Box("K")) { arg1, int, arg2 ->
        "$arg1$arg2"
    }.value
}
