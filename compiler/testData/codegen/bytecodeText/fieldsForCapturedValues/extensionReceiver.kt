class Receiver {
    fun foo() {}
}

fun Receiver.bar() {
    class NamedLocal {
        fun run() {
            foo()
        }
    }
}

// 1 final synthetic LReceiver; \$this_bar
