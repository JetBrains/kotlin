class C() {
    default object Foo {
        fun create() = 3
    }
}

fun box(): String {
    val c1 = C.Foo.create()
    val c2 = C.create()
    return if (c1 == 3 && c2 == 3) "OK" else "fail"
}

