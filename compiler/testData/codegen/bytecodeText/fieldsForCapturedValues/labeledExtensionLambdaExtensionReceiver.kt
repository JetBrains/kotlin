class Receiver {
    fun foo() {}
}

fun useExtensionLambda(lambda: Receiver.() -> Unit) {
}

fun test() {
    useExtensionLambda label@ {
        class NamedLocal {
            fun run() {
                foo()
            }
        }
    }
}

// 1 final synthetic LReceiver; \$this
