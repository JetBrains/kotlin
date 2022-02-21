// IGNORE_BACKEND_FIR: JVM_IR

abstract class A<T> {
    fun print() = "OK"
}

fun f() = test("")

private fun <T> test(t: T) =
    object : A<T>() {}


fun box() = f().print()
