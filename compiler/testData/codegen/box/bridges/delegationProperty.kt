// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    var result: T
}

class B(a: A<String>): A<String> by a

fun box(): String {
    val o = object : A<String> {
        override var result = "Fail"
    }
    val b: A<String> = B(o)
    b.result = "OK"
    if (b.result != "OK") return "Fail"
    return b.result
}
