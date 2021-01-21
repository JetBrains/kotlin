// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// JVM_TARGET: 1.6

interface I
class C : I { fun foo() = super<I>.hashCode() }

fun box(): String {
    C().foo()
    return "OK"
}
