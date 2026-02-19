class MyClass {
    operator fun plus(a: Int): MyClass = this
    operator fun unaryMinus(): MyClass = this
}

var variable: MyClass
    get() = MyClass()
    set(value) {}

fun main() {
    -<expr>variable</expr>
}