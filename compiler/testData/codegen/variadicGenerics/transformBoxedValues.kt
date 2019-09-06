// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Box<T>(val value: T)

fun <T, R, vararg Ts> Box<T>.withOthers (
    vararg others: *Box<Ts>,
    transform: (T, *Ts) -> R
): Box<R> {
    val args = Tuple<Ts>(others.size)
    for (i in 0 until others.size) {
        args[i] = others.get<Box<Any?>>(i).value
    }
    return Box(transform(this.value, args))
}

fun box(): String {
    return Box("O").withOthers(Box(15), Box("K")) { arg1, int, arg2 ->
        arg1 + arg2
    }.value
}
