abstract class Base(val fn: () -> String)

interface Host {
    companion object : Base(run { { Host.ok() } }) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()