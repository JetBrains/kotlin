package runtime.basic.statements0

import kotlin.test.*

fun simple() {
    var a = 238
    a++
    println(a)
    --a
    println(a)
}

class Foo() {
    val j = 2
    var i = 29

    fun more() {
        i++
    }

    fun less() {
        --i
    }
}

fun fields() {
    val foo = Foo()
    foo.more()
    println(foo.i)
    foo.less()
    println(foo.i)
}

@Test fun runTest() {
    simple()
    fields()
}