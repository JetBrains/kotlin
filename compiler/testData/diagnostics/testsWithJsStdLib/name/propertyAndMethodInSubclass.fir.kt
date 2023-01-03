package foo

open class Super {
    val foo = 23
}

class Sub : Super() {
    fun foo() = 42
}
