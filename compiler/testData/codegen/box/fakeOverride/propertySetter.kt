// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    var result: String
}

open class A : T {
    override var result: String
        get() = ""
        set(value) {}
}

class B : A(), T
class C : T, A()

fun box(): String {
    B().result = ""
    C().result = ""
    return "OK"
}
