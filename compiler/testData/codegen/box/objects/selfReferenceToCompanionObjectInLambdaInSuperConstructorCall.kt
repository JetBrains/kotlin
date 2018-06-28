// IGNORE_BACKEND: JVM_IR
abstract class Base(val fn: () -> String)

class Host {
    companion object : Base({ Host.ok() }) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()