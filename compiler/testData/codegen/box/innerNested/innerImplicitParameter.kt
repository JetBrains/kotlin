open class C<T> {
    inner class A<U>(val x: T?, val y: U)

    class D : C<Nothing>() {
        fun f() = A<String>(null, "OK")
    }
}

fun box(): String {
    return C.D().f().y
}
