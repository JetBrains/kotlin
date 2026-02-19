class MyClass {
    operator fun get(i: Int): MyClass = this
    operator fun set(i: Int, v: MyClass) {}
    operator fun plus(v: MyClass): MyClass = this
}

var variable: MyClass
    get() = MyClass()
    set(value) {}

fun main() {
    variable[0] <expr>+=</expr> MyClass()
}