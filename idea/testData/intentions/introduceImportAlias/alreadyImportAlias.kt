import Outer.Middle as P

class Outer {
    class Middle {
        class Inner
    }
}

class Test() {
    fun test() {
        val i = Outer.Middle<caret>.Inner()
    }
}