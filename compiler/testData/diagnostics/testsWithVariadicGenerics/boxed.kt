// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNCHECKED_CAST -UNUSED_VARIABLE

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

fun test(): String {
    return Box("O").withOthers(
        Box(15),
        Box("K")
    ) { first, int, second ->
        val newInt = int + 2
        first + second
    }.value
}