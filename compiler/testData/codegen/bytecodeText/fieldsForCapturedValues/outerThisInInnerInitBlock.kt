class Outer {
    inner class Inner {
        init {
            class NamedLocal {
                fun foo() {
                    outer()
                }
            }
        }
    }

    fun outer() {}
}

// JVM_TEMPLATES
// 1 final synthetic LOuter\$Inner; this\$0

// JVM_IR_TEMPLATES
// 2 final synthetic LOuter; this\$0
