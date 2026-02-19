class MyClass {
    operator fun dec(): MyClass = this
}

var variable: MyClass
    get() = MyClass()
    set(value) {}

fun main() {
    <expr>--</expr>variable
}
