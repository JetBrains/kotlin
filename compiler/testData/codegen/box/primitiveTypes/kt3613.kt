// IGNORE_BACKEND_FIR: JVM_IR
fun foo(): Int? = 42

fun box(): String {
    if (foo()!! > 239) return "Fail"
    return "OK"
}
