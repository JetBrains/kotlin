// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

inline class S(val value: String?) {
    operator fun plus(other: S): S = S(this.value!! + other.value!!)
}

object C {
    @JvmStatic
    fun foo(x: S, y: String, z: S?): S = x + S(y) + z!!
}

interface I {
    companion object {
        @JvmStatic
        fun bar(x: String, y: S, z: S?): S = S(x) + y + z!!
    }
}

fun box(): String {
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("abc"), C::foo.call(S("a"), "b", S("c")))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("def"), (I)::bar.call("d", S("e"), S("f")))
    }

    val unboundFoo = C::class.members.single { it.name == "foo" } as KFunction<*>
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("ghi"), unboundFoo.call(C, S("g"), "h", S("i")))
    }

    val unboundBar = I.Companion::class.members.single { it.name == "bar" } as KFunction<*>
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("jkl"), unboundBar.call(I, "j", S("k"), S("l")))
    }

    return "OK"
}
