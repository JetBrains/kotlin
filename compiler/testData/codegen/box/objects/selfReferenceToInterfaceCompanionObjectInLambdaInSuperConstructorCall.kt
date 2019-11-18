// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val fn: () -> String)

interface Host {
    companion object : Base({ Host.ok() }) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()