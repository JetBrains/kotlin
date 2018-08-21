package codegen.inline.classDeclarationInsideInline

import kotlin.test.*

fun f() {
    run {
        class Test1<T : Number, G>(val x: T, val y: G) {
            override fun toString() = "test1: ${x.toDouble()}"
        }

        class Test2<X>(val a: Test1<Int, X>) {
            override fun toString() = "test2"
        }

        val v = Test2(Test1(1, Test2(Test1(1, 3))))
        println(v.a)
        println(v.a.x)
        println(v.a.y)
    }
}

@Test fun test() {
    f()
}
