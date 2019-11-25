// IGNORE_BACKEND_FIR: JVM_IR
import Foo.bar0 as bar

object Foo {
    val bar0 = "OK"

    fun test() = bar0
}

fun box() = Foo.test()