// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: [IR VALIDATION] Duplicate IR node: FUN GENERATED_DATA_CLASS_MEMBER name:equals

interface A {
    fun Any.equals(other: Any?): Boolean = false
    fun Any.hashCode(): Int = 0
    fun Any.toString(): String = ""
}

data class B(val x: Int) : A

fun box(): String {
    if (B(42) != B(42)) return "Fail equals"
    if (B(42).hashCode() != B(42).hashCode()) return "Fail hashCode"
    if (B(42).toString() != B(42).toString()) return "Fail toString"
    return "OK"
}
