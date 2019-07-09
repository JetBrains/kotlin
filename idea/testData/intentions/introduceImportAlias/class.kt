import Outer.Inner

class Outer {
    class Inner
}

class Test(){
    fun test(){
        val i = Inner<caret>()
    }
    fun test2(){
        val i = Inner()
    }
}