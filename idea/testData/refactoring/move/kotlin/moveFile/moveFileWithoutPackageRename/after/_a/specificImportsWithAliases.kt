package a

import a.Test as _Test
import a.test as _Test
import a.TEST as _TEST

fun bar() {
    val t: _Test = _Test()
    _test()
    println(_TEST)
    _TEST = ""
}
