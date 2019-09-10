// !LANGUAGE: +NewInference +VariadicGenerics
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Box<T>(val value: T)

fun <R, vararg Ts> variadic (
    vararg arguments: *Box<Box<Ts>>,
    transform: (*Box<Ts>) -> R
): R {
    val args = Tuple<Box<Ts>>(arguments.size)
    for (i in 0 until arguments.size) {
        args[i] = arguments.get<Box<Any?>>(i).value
    }
    return transform(args)
}

fun box(): String {
    return variadic(Box(Box("O")), Box(Box("K"))) { boxO, boxK ->
        boxO.value + boxK.value
    }
}