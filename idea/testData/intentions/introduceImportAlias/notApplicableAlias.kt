// IS_APPLICABLE: false
import Outer.Inner as NotTestAlias

class Outer {
    class Inner
}

class Test() {
    fun test() {
        val i = NotTestAlias<caret>()
    }

    fun test2() {
        val i = NotTestAlias()
    }
}