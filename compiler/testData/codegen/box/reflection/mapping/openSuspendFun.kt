// WITH_REFLECT
// TARGET_BACKEND: JVM

import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals

open class Aaa {
    suspend open fun aaa() {}
}

class Bbb {
    suspend fun bbb() {}
}

fun box(): String {
    val bbb = Bbb::class.declaredMemberFunctions.first { it.name == "bbb" }.javaMethod
    assertEquals("bbb", bbb!!.name)
    val aaa = Aaa::class.declaredMemberFunctions.first { it.name == "aaa" }.javaMethod
    assertEquals("aaa", aaa!!.name)
    return "OK"
}
