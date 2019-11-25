// IGNORE_BACKEND_FIR: JVM_IR
class A

fun A.foo() = (A::bar)(this, "OK")

fun A.bar(x: String) = x

fun box() = A().foo()
