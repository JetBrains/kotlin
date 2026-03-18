// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals

interface Variance<A, in B, out C, D>
class OneBound<T : Enum<T>>
class SeveralBounds<T : Cloneable> where T : Enum<T>, T : Variance<String, Int?, Double?, Number>

fun box(): String {
    assertEquals("[A, in B, out C, D]", Variance::class.typeParameters.toString())
    assertEquals("[T]", OneBound::class.typeParameters.toString())
    assertEquals("[T]", SeveralBounds::class.typeParameters.toString())

    return "OK"
}
