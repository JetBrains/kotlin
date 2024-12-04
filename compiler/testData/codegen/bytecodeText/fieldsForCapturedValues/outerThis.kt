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

// 2 final synthetic LOuter; this\$0
