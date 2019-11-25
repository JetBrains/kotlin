// IGNORE_BACKEND_FIR: JVM_IR
enum class A { V }

fun box(): String {
    val a: A = A.V
    when (a) {
        A.V -> return "OK"
    }
}