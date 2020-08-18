class Host {
    private fun bar() {
        class NamedLocal {
            fun run() {
                foo()
            }
        }
    }

    fun foo() {}
}

// JVM_TEMPLATES
// 1 final synthetic LHost; this\$0

// JVM_IR_TEMPLATES
// 1 private final synthetic LHost; this\$0