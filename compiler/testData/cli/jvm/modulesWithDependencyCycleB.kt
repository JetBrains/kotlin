package b

import a.*

class B(val a: A? = null)

open class Y(val x: X? = null)

fun topLevelB() {
    topLevelA2()
}
