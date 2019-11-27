// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    var v: T
}

class B : A<String> {
    override var v: String = "Fail"
}

fun box(): String {
    val a: A<String> = B()
    a.v = "OK"
    return a.v
}
