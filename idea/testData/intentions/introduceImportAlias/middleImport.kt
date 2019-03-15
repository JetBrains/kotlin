import Outer.Middle

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
        val i = Middle<caret>.Inner.SIZE
    }

    fun test2() {
        val i = Outer.Middle.Inner.SIZE
    }
}