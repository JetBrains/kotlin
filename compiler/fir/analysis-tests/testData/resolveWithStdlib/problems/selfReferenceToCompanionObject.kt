abstract class Base(val fn: () -> String)

class Host {
    companion object : Base(run { { Host.ok() } }) {
        fun ok() = "OK"
    }
}

enum class Test(val x: String, val closure1: () -> String) {
    FOO("O", run { { FOO.x } }) {
        override val y: String = "K"
        val closure2 = { y } // Implicit 'FOO'
        override val z: String = closure2()
    };

    abstract val y: String
    abstract val z: String
}

fun box() = Host.Companion.fn()
