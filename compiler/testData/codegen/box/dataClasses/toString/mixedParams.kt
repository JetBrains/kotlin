// IGNORE_BACKEND_FIR: JVM_IR
data class A(var x: Int, val z: Int?)

fun box(): String {
    val a = A(1, null)
    if("$a" != "A(x=1, z=null)") return "$a"
    return "OK"
}
