// IGNORE_BACKEND_FIR: JVM_IR
object A {
    class B
    class C<T>
}

fun box(): String {
    val b = A.B()
    val c = A.C<String>()
    return "OK"
}
