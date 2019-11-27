// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun Int.is42With(that: Int) = this + 2 * that == 42
    return if ((Int::is42With)(16, 13)) "OK" else "Fail"
}
