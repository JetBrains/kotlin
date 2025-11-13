// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82376

abstract class A<T> {
    fun print() = "OK"
}

fun f() = test("")

private fun <T> test(t: T) =
    object : A<T>() {}


fun box() = f().print()
