// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

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
        "C<I1, I2, I3?, in I4, out I5, I6, I7?>",
        typeOf<C<I1, I2, I3?, in I4, out I5, I6, I7?>>().toString()
    )
    assertEquals(
        "C<out I1, I2?, I3, I4, I5, I6?, in I7>?",
        typeOf<C<out I1, I2?, I3, I4, I5, I6?, in I7>?>().toString()
    )

    return "OK"
}
