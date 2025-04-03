// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

interface A<A1, A2>
interface B<B1, B2> : A<B2 & Any, B1?>

class C<T> : B<Any, T?>
class D<U> : B<U & Any, List<U?>>

fun box(): String {
    assertEquals(
        "[test.B<kotlin.Any, T?>, test.A<T & Any, kotlin.Any?>, kotlin.Any]",
        C::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.B<U & Any, kotlin.collections.List<U?>>, test.A<kotlin.collections.List<U?>, U?>, kotlin.Any]",
        D::class.allSupertypes.toString(),
    )

    return "OK"
}
