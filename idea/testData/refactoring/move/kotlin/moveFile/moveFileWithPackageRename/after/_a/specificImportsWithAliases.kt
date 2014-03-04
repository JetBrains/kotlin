package a

import b.Test as _Test
import b.test as _Test
import b.TEST as _TEST

fun bar() {
    val t: _Test = _Test()
    _test()
    println(_TEST)
    _TEST = ""
}
