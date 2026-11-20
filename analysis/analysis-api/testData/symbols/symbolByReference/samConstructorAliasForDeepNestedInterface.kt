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

typealias MyTypeAlias = Outer.Nested.Companion.Inner.Companion.FunInterface

fun usage() {
    MyType<caret>Alias { }
}
