package test

import kotlin.reflect.KClass
import kotlin.test.*

class K

class Test {
    fun kClass(): Any = K::class

    fun doTest(k1: KClass<*>, k2: KClass<*>) {
        // KClass instances should be equal for classes loaded with the child and the parent
        assertEquals(k1, k2)
    }
}
