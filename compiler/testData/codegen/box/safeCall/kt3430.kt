// IGNORE_BACKEND_FIR: JVM_IR
fun f(b : Int.(Int)->Int) = 1?.b(1)

fun box(): String {
    val x = f { this + it }
    return "OK"
}