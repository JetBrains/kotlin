package test

import kotlin.reflect.KClass
import kotlin.test.*

class K(val p: String)

class Test {
    fun kClass(): Any = K::class

    fun doTest(k1: KClass<*>, k2: KClass<*>) {
        // KClass instances for classes loaded with different class loaders should have the same string representation,
        // but should not be equal
        assertEquals("$k1", "$k2")
        assertNotEquals(k1, k2)

        // The same for properties of these classes
        val p1 = k1.properties.first()
        val p2 = k2.properties.first()
        assertEquals("$p1", "$p2")
        assertNotEquals(p1, p2)
    }
}
