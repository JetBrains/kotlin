import Outer.Middle as Middle1

class Outer {
    class Middle {
        class Inner {
            companion object {
                const val SIZE = 1
            }
        }
    }
}

class Test() {
    fun test() {
        val i = Outer.Middle<caret>.Inner.SIZE
    }

    fun test2() {
        val i = Middle1.Inner.SIZE
    }
}