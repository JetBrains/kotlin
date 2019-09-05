// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Box<T>(val value: T)

fun <R, vararg Ts> variadic (
    vararg arguments: *Box<Box<Ts>>,
    transform: (*Box<Ts>) -> R
): R {
    val args = Tuple<Any?>(arguments.size)
    for (i in 0 until arguments.size) {
        args[i] = (arguments[i] as Box<Any?>).value
    }
    return transform(args as Tuple<Box<Ts>>)
}

fun box(): String {
    return variadic(Box(Box("O")), Box(Box("K"))) { boxO, boxK ->
        boxO.value + boxK.value
    }
}