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

// JVM_TEMPLATES
// 1 final synthetic LReceiver; \$this_bar

// JVM_IR_TEMPLATES
// 1 private final synthetic LReceiver; \$this_bar