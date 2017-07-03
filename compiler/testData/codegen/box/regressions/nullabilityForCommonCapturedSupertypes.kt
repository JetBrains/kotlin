val targetArgument = id2(star(), star()) // error

fun <T> id2(x: T, y: T): T = x

fun star(): Sample<*> {
    return Sample<Int>()
}

class Sample<out T>

fun box(): String {
    targetArgument
    return "OK"
}