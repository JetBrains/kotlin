// IGNORE_BACKEND_FIR: JVM_IR
package pack

open class A(val value: String )

class B(value: String) : A(value) {
    override fun toString() = "B($value)";
}

fun box() = if (B("4").toString() == "B(4)") "OK" else "fail"
