class A {
    operator fun unaryPlus(): A = this
    operator fun unaryMinus(): A = this
    operator fun not(): Boolean = true
    operator fun inc(): A = this
    operator fun dec(): A = this
}

fun test() {
    var a = A()
    +a
    -a
    !a
    ++a
    --a
    a++
    a--
}
