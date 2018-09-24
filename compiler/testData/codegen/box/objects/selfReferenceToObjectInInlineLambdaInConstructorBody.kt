// IGNORE_BACKEND: JVM_IR
object Test {
    fun ok() = "OK"
    val x = run { Test.ok() }
    fun test() = x
}

fun box() = Test.test()