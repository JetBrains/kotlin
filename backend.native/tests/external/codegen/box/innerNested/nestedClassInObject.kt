object A {
    class B
    class C<T>
}

fun box(): String {
    val b = A.B()
    val c = A.C<String>()
    return "OK"
}
