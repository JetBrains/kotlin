// IGNORE_BACKEND: JVM_IR
abstract class Base(val fn: () -> String)

object Test : Base({ Test.ok() }) {
    fun ok() = "OK"
}

fun box() = Test.fn()