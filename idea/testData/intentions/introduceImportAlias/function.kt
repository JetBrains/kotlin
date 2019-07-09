import Outer.Middle.Inner
import Outer.Middle.Inner.Companion.foo

class Outer {
    class Middle {
        class Inner {
            companion object {
                fun foo() {}
                fun foo(a: Outer) {}
            }
        }
    }
}

class Test() {
    fun test() {
        val i = Inner.foo<caret>()
    }

    fun test2() {
        val i = Outer.Middle.Inner.foo(Outer())
    }
}