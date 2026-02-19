class Outer {
    inner class Inner {
        constructor() {
            class NamedLocal {
                fun foo() {
                    outer()
                }
            }
        }
    }

    fun outer() {}
}

// 2 final synthetic LOuter; this\$0
