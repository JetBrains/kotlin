package test

class Outer {
    class Nested {
        companion object {
            class Inner {
                companion object {
                    fun interface FunInterface {
                        fun f()
                    }
                }
            }
        }
    }
}

fun usage() {
    Outer.Nested.Companion.Inner.Companion.FunIn<caret>terface { }
}
