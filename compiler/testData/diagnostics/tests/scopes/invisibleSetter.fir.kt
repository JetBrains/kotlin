class A<T> {
    public var x: Int = 0
        private set
}

fun main() {
    val a = A<Any>()
    a.<!INVISIBLE_SETTER!>x<!> = 1
}
