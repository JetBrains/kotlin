class Outer {
    inner class Inner {
        private fun bar() {
            class NamedLocal {
                fun run() {
                    innerFoo()
                    outerFoo()
                }
            }
        }

        fun innerFoo() {}
    }

    fun outerFoo() {}
}

// JVM_TEMPLATES
// 1 final synthetic LOuter\$Inner; this\$0

// JVM_IR_TEMPLATES
// 1 private final synthetic LOuter\$Inner; this\$0
// 1 private final synthetic LOuter; this\$1