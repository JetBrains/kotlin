// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class A {
    init{
        sb.appendLine ("A::init")
    }

    val a = 1

    companion object :B(1) {
        init {
            sb.appendLine ("A::companion")
        }

        fun foo() {
            sb.appendLine("A::companion::foo")
        }
    }

    object AObj : B(){
        init {
            sb.appendLine("A::Obj")
        }
        fun foo() {
            sb.appendLine("A::AObj::foo")
        }
    }
}

fun box(): String {
    sb.appendLine("main")
    A.foo()
    A.foo()
    A.AObj.foo()
    A.AObj.foo()

    assertEquals("""
        main
        B::constructor(1)
        A::companion
        A::companion::foo
        A::companion::foo
        B::constructor(0)
        B::constructor()
        A::Obj
        A::AObj::foo
        A::AObj::foo

    """.trimIndent(), sb.toString())
    return "OK"
}

open class B(val a:Int, val b:Int) {
    constructor(a:Int):this (a, 0) {
        sb.appendLine("B::constructor(" + a.toString()+ ")")
    }
    constructor():this(0) {
        sb.appendLine("B::constructor()")
    }
}
