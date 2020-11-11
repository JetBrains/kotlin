// TARGET_BACKEND: JVM
// WITH_REFLECT
// IGNORE_BACKEND_FIR: JVM_IR

package test

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

inline class Z(val value: String)

class S {
    suspend fun consumeZ(z: Z) {}
    suspend fun produceZ(): Z = Z("")
    suspend fun consumeAndProduceZ(z: Z): Z = z
}

fun box(): String {
    val members = S::class.members.filterIsInstance<KFunction<*>>().associateBy(KFunction<*>::name)

    members["consumeZ"]!!.let { cz ->
        val czj = cz.javaMethod!!
        assertTrue(czj.name.startsWith("consumeZ-"), czj.name)
        assertEquals("java.lang.String, kotlin.coroutines.Continuation", czj.parameterTypes.joinToString { it.name })
        val czjk = czj.kotlinFunction
        assertEquals(cz, czjk)
    }

    members["produceZ"]!!.let { pz ->
        val pzj = pz.javaMethod!!
        assertTrue(pzj.name.startsWith("produceZ-"), pzj.name)
        assertEquals("kotlin.coroutines.Continuation", pzj.parameterTypes.joinToString { it.name })
        val pzjk = pzj!!.kotlinFunction
        assertEquals(pz, pzjk)
    }

    members["consumeAndProduceZ"]!!.let { cpz ->
        val cpzj = cpz.javaMethod!!
        assertTrue(cpzj.name.startsWith("consumeAndProduceZ-"), cpzj.name)
        assertEquals("java.lang.String, kotlin.coroutines.Continuation", cpzj.parameterTypes.joinToString { it.name })
        val cpzjk = cpzj!!.kotlinFunction
        assertEquals(cpz, cpzjk)
    }

    return "OK"
}
