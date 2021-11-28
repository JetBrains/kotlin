// WITH_STDLIB

interface A<T> {
    var x: T
}

fun test(a: A<*>) {
    a.x
}

fun box(): String {
    return "OK"
}
