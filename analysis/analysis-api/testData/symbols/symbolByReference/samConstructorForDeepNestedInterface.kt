// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

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
