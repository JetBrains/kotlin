// ISSUE: KT-68523
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: Unknown classifier kind null

data class Some<T>(val data: T & Any)

fun box(): String {
    val x = Some<String?>("Fail")
    val y = x.copy(data = "OK")
    if (x == y) {
        return "Fail: equals"
    }
    y.hashCode()
    return y.component1()
}
