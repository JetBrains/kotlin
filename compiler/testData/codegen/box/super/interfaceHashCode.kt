// IGNORE_BACKEND_FIR: JVM_IR
interface I
class C : I { fun foo() = super<I>.hashCode() }

fun box(): String {
    C().foo()
    return "OK"
}
