package usage

import a.*

fun baz(param: A, nested: A.Nested) {
    val constructor = A()
    val nested = A.Nested()
    val methodCall = param.method()
    val supertype = object : A() {}

    val x = foo()
    val y = bar
    bar = 239
    val z: TA = ""
}
