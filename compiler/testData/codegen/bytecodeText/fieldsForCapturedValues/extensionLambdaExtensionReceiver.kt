class Receiver {
    fun foo() {}
}

fun useExtensionLambda(lambda: Receiver.() -> Unit) {
}

fun test() {
    useExtensionLambda { 
        class NamedLocal {
            fun run() {
                foo()
            }
        }
    }
}

// JVM_TEMPLATES
// 1 final synthetic LReceiver; \$this_useExtensionLambda

// JVM_IR_TEMPLATES
// 1 final synthetic LReceiver; \$this