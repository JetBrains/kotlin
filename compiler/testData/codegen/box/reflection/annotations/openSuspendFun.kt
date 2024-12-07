// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.declaredMemberFunctions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

annotation class Anno

open class Aaa {
    @Anno
    suspend open fun aaa() {}
}

class Bbb {
    @Anno
    suspend fun bbb() {}
}

fun box(): String {
    val bbb = Bbb::class.declaredMemberFunctions.first { it.name == "bbb" }.annotations
    assertEquals(1, bbb.size)
    assertTrue(bbb.single() is Anno)
    val aaa = Aaa::class.declaredMemberFunctions.first { it.name == "aaa" }.annotations
    assertEquals(1, aaa.size)
    assertTrue(aaa.single() is Anno)
    return "OK"
}
