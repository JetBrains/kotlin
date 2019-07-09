// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

const val const = "const"
val nonConst = "nonConst"

class A {
    lateinit var lateinit: Unit
    var nonLateinit = Unit
}

fun box(): String {
    assertTrue(::const.isConst)
    assertFalse(::nonConst.isConst)

    assertTrue(A::lateinit.isLateinit)
    assertFalse(A::nonLateinit.isLateinit)

    return "OK"
}
