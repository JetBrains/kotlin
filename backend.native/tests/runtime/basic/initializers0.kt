class A {
    init{
        println ("A::init")
    }

    val a = 1

    companion object :B(1) {
        init {
            println ("A::companion")
        }

        fun foo() {
            println("A::companion::foo")
        }
    }

    object AObj : B(){
        init {
            println("A::Obj")
        }
        fun foo() {
            println("A::AObj::foo")
        }
    }
}

fun main(args:Array<String>) {
    println("main")
    A.foo()
    A.foo()
    A.AObj.foo()
    A.AObj.foo()
}

open class B(val a:Int, val b:Int) {
    constructor(a:Int):this (a, 0) {
        println("B::constructor(" + a.toString()+ ")")
    }
    constructor():this(0) {
        println("B::constructor()")
    }
}

