class Outer {
    inner class Inner {
        private fun bar() {
            class NamedLocal {
                fun run() {
                    foo()
                }
            }
        }
    }

    fun foo() {}
}

// JVM_TEMPLATES
// 1 final synthetic LOuter\$Inner; this\$0

// JVM_IR_TEMPLATES
// 2 final synthetic LOuter; this\$0
