// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    var x: T
}

class B(override var x: String) : A<String>

fun box(): String {
    val a: A<String> = B("Fail")
    a.x = "OK"
    return a.x
}
