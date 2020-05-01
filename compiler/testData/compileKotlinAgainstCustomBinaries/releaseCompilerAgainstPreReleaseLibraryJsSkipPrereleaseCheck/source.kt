@file:Suppress("UNUSED_VARIABLE")
package usage

import a.*

fun baz(param: A) {
    val constructor = A()
    val methodCall = param.hashCode()
    val supertype = object : A() {}

    val x = foo()
    val y = bar
    bar = 239
    val z: TA = ""
}
