// TARGET_BACKEND: JVM

// WITH_REFLECT

import java.util.Collections
import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class A private constructor(x: Int) {
    public constructor(s: String): this(s.length)
    constructor(): this("")
}

class B : A("")

class C {
    class Nested
    inner class Inner
}

fun box(): String {
    assertEquals(3, A::class.constructors.size)
    assertEquals(1, B::class.constructors.size)

    assertTrue(Collections.disjoint(A::class.members, A::class.constructors))
    assertTrue(Collections.disjoint(B::class.members, B::class.constructors))

    assertEquals(1, C.Nested::class.constructors.size)
    assertEquals(1, C.Inner::class.constructors.size)

    return "OK"
}
