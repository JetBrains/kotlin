// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

inline class Z(val value: Int) {
    operator fun plus(other: Z): Z = Z(this.value + other.value)
}

object C {
    @JvmStatic
    fun foo(x: Z, y: Int, z: Z?): Z = x + Z(y) + z!!
}

interface I {
    companion object {
        @JvmStatic
        fun bar(x: Int, y: Z, z: Z?): Z = Z(x) + y + z!!
    }
}

fun box(): String {
    val one = Z(1)
    val two = Z(2)
    val four = Z(4)
    val seven = Z(7)

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(seven, C::foo.call(one, 2, four))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(seven, (I)::bar.call(1, two, four))
    }

    val unboundFoo = C::class.members.single { it.name == "foo" } as KFunction<*>
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(seven, unboundFoo.call(C, one, 2, four))
    }

    val unboundBar = I.Companion::class.members.single { it.name == "bar" } as KFunction<*>
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(seven, unboundBar.call(I, 1, two, four))
    }

    return "OK"
}
