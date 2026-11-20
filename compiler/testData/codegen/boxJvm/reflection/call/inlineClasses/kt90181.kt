// LANGUAGE: +CompanionBlocks
// WITH_REFLECT

import kotlin.reflect.full.staticFunctions
import kotlin.test.assertEquals

@JvmInline
value class V(val v: Int) {
    companion {
        fun of(v: Int): V = V(v)
        fun plain(v: Int): Int = v
    }
}

class Regular {
    companion {
        fun of(v: Int): V = V(v)
    }
}

fun box(): String {
    assertEquals(V(1), V::of.call(1))
    assertEquals(1, V::plain.call(1))
    assertEquals(V(1), Regular::of.call(1))

    assertEquals(V(1), V::class.staticFunctions.single { it.name == "of" }.call(1))
    assertEquals(1, V::class.staticFunctions.single { it.name == "plain" }.call(1))
    assertEquals(V(1), Regular::class.staticFunctions.single { it.name == "of" }.call(1))

    return "OK"
}
