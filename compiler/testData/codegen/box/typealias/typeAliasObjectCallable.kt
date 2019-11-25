// IGNORE_BACKEND_FIR: JVM_IR
object O {
    val x = "OK"

    operator fun invoke() = x
}

typealias A = O

fun box(): String = A()
