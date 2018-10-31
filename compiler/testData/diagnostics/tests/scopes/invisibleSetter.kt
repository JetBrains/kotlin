class A<T> {
    public var x: Int = 0
        private set
}

fun main() {
    val a = A<Any>()
    <!INVISIBLE_SETTER!>a.x<!> = 1
}