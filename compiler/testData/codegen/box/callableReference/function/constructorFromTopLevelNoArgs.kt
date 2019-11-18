// IGNORE_BACKEND_FIR: JVM_IR
class A {
    var result = "OK"
}

fun box() = (::A)().result
