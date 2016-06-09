class Cell<TC>(val x: TC)

object OkHost {
    val value = "OK"
}

fun <T> id(x: T): T {
    typealias C = Cell<T>
    val c: C = C(x)
    return c.x
}

fun box(): String {
    typealias OK = OkHost
    return id(OK.value)
}