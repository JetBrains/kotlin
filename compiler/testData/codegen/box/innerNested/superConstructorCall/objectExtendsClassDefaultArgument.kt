// IGNORE_BACKEND_FIR: JVM_IR
// KT-3581

open class A(val result: String = "OK") {
}

fun box(): String {
    val a = object : A() {}
    return a.result
}
