// IGNORE_BACKEND: JS, JS_IR, WASM
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

interface I1
interface I2
interface I3
interface I4
interface I5
interface I6
interface I7

class C<T1, T2, T3, T4, T5, T6, T7>

fun box(): String {
    assertEquals(
        "test.C<test.I1, test.I2, test.I3?, in test.I4, out test.I5, test.I6, test.I7?>",
        typeOf<C<I1, I2, I3?, in I4, out I5, I6, I7?>>().toString()
    )
    assertEquals(
        "test.C<out test.I1, test.I2?, test.I3, test.I4, test.I5, test.I6?, in test.I7>?",
        typeOf<C<out I1, I2?, I3, I4, I5, I6?, in I7>?>().toString()
    )

    return "OK"
}
