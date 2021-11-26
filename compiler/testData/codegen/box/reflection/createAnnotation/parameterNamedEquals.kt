// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

annotation class Anno(val equals: Boolean)

fun box(): String {
    val t = Anno::class.constructors.single().call(true)
    val f = Anno::class.constructors.single().call(false)
    assertEquals(true, t.equals)
    assertEquals(false, f.equals)
    assertNotEquals(t, f)
    return "OK"
}
