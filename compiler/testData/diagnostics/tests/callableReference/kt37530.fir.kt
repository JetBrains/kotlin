
abstract class Abstract

fun <D> create(fn: () -> D): D {
    return fn()
}

fun main() {
    create(::Abstract)
}
