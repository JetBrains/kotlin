// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

package test

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JvmInline
value class Z(val value1: UInt, val value2: String)

class S {
    suspend fun consumeZ(z: Z) {}
    suspend fun produceZ(): Z = Z(0U, "")
    suspend fun consumeAndProduceZ(z: Z): Z = z
}

fun box(): String {
    val members = S::class.members.filterIsInstance<KFunction<*>>().associateBy(KFunction<*>::name)

    members["consumeZ"]!!.let { cz ->
        val czj = cz.javaMethod!!
        assertTrue(czj.name.startsWith("consumeZ-"), czj.name)
        assertEquals("int, java.lang.String, kotlin.coroutines.Continuation", czj.parameterTypes.joinToString { it.name })
        val czjk = czj.kotlinFunction
        assertEquals(cz, czjk)
    }

    members["produceZ"]!!.let { pz ->
        val pzj = pz.javaMethod!!
        assertEquals("produceZ", pzj.name)
        assertEquals("kotlin.coroutines.Continuation", pzj.parameterTypes.joinToString { it.name })
        val pzjk = pzj!!.kotlinFunction
        assertEquals(pz, pzjk)
    }

    members["consumeAndProduceZ"]!!.let { cpz ->
        val cpzj = cpz.javaMethod!!
        assertTrue(cpzj.name.startsWith("consumeAndProduceZ-"), cpzj.name)
        assertEquals("int, java.lang.String, kotlin.coroutines.Continuation", cpzj.parameterTypes.joinToString { it.name })
        val cpzjk = cpzj!!.kotlinFunction
        assertEquals(cpz, cpzjk)
    }

    return "OK"
}
