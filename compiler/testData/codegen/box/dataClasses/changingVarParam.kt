// IGNORE_BACKEND_FIR: JVM_IR
data class A(var string: String)

fun box(): String {
    val a = A("Fail")
    a.string = "OK"
    val (result) = a
    return result
}
