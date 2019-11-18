// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Unit)

fun box(): String {
    val a = A(Unit)
    return if ("$a" == "A(x=kotlin.Unit)") "OK" else "$a"
}
