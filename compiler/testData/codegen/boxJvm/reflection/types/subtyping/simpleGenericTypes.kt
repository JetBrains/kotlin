// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

open class G<T>
class A : G<String>()

fun gOfString(): G<String> = null!!
fun gOfInt(): G<Int> = null!!

fun box(): String {
    val gs = ::gOfString.returnType
    val gi = ::gOfInt.returnType
    val a = ::A.returnType

    assertTrue(a.isSubtypeOf(gs))
    assertTrue(gs.isSupertypeOf(a))

    assertFalse(a.isSubtypeOf(gi))
    assertFalse(gi.isSupertypeOf(a))

    assertFalse(gs.isSubtypeOf(gi))
    assertFalse(gs.isSupertypeOf(gi))
    assertFalse(gi.isSubtypeOf(gs))
    assertFalse(gi.isSupertypeOf(gs))

    return "OK"
}
