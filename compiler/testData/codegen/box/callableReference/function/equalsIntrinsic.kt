// IGNORE_BACKEND_FIR: JVM_IR
class A

fun box() = if ((A::equals)(A(), A())) "Fail" else "OK"
