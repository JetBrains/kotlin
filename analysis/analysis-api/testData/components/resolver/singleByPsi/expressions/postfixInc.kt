class MyClass {
    operator fun inc(): MyClass = this
}

var variable: MyClass
    get() = MyClass()
    set(value) {}

fun main() {
    <expr>variable++</expr>
}
