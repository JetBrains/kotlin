// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Box<T>(val value: T)

class Collector<R: Any> {
    lateinit var value: R
    fun collect(value: R) {
        this.value = value
    }
}

fun box(): String {
    val boxedResult = transformAndCollect(
        Box("O"), Box(41), Box(Unit), Box("K")
    ) { o, int, _, k ->
        int.inc()
        o + k
    }
    return boxedResult.value
}

fun <vararg Ts, R: Any> transformAndCollect(
    vararg args: *Box<Ts>,
    transform: (*Ts) -> R
): Box<R> {
    val collector = Collector<R>()
    with(collector) {
        transformOnCollector(
            *args,
            transform = { collect(transform(it)) }
        )
    }
    return Box(collector.value)
}

fun <vararg Ts, R: Any> Collector<R>.transformOnCollector(
    vararg args: *Box<Ts>,
    transform: Collector<R>.(*Ts) -> Unit
) {
    val arguments = Tuple<Ts>(args.size)
    for (index in 0 until args.size) {
        arguments[index] = (args[index] as Box<Any?>).value
    }
    transform(arguments)
}
