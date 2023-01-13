package p

abstract class A {
    open internal val ap: Int = 4
    abstract internal fun af(): Int
}

interface I {
    internal val ip: Int
    internal fun if(): Int
}