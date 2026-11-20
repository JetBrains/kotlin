// LANGUAGE: +ContextParameters

class Box(var value: Int)

var getContexts = 0

context(c: Int)
operator fun Box.get(index: Int): Int {
    getContexts += c
    return value
}

operator fun Box.set(index: Int, v: Int) {
    value = v
}

fun box(): String {
    val b = Box(1)
    with(10) {
        b[0] += 1
    }
    return if (b.value == 2 && getContexts == 10) "OK" else "Fail: ${b.value} $getContexts"
}
