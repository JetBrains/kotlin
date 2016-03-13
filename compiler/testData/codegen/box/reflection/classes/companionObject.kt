// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.*

class A {
    companion object C
}

enum class E {
    ENTRY;
    companion object {}
}

fun box(): String {
    val obj = A::class.companionObject
    assertNotNull(obj)
    assertEquals("C", obj!!.simpleName)

    assertEquals(A.C, A::class.companionObjectInstance)
    assertEquals(A.C, obj.objectInstance)

    assertNull(A.C::class.companionObject)
    assertNull(A.C::class.companionObjectInstance)

    assertEquals(E.Companion, E::class.companionObjectInstance)

    return "OK"
}
