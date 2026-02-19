fun usage() {
    val one = 1
    val negative = -one
    val positive = +one

    !(+(-MyClass().unaryMinus().not()))
}

class MyClass {
    operator fun unaryMinus(): MyClass = this
    operator fun unaryPlus(): MyClass = this
    operator fun not(): MyClass = this
}