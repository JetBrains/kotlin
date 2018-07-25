// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
abstract class Base(val fn: () -> String)

class Host {
    companion object : Base(run { { Host.ok() } }) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()