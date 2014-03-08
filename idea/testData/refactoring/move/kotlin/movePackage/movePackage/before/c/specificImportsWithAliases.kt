package c

import a.A as AA
import a.foo as foofoo
import a.x as xx

fun bar() {
    val t: AA = AA()
    foofoo()
    println(xx)
    xx = ""
}
