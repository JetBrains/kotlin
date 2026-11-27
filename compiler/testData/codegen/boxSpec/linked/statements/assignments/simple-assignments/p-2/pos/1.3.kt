// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, simple-assignments -> paragraph 2 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: If a property has a setter (including delegated properties), it is called using the right-hand side expression as its argument
 */

import kotlin.reflect.KProperty

var flag = false
var data = "FooBoo"

fun box(): String {
    val e = Example()
    assert(!flag)
    e.p = data
    if (flag && e.p == data) return "OK"
    return "NOK"
}

class Example {
    var p: String by Delegate()
}


class Delegate {
    var d: String? = null
    operator fun getValue(example: Example, property: KProperty<*>): String {
        return d.toString()
    }
    operator fun setValue(example: Example, property: KProperty<*>, s: String) {
        flag = true
        d = s
    }
}

