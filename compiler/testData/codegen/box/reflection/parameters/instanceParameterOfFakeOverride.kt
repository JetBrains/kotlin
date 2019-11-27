// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

open class A {
    val property = "OK"

    fun function() {}
}

class B : A()

fun box(): String {
    assertEquals(B::class, B::property.instanceParameter!!.type.jvmErasure)
    assertEquals(B::class.java, B::property.instanceParameter!!.type.javaType)
    assertEquals(B::class, B::function.instanceParameter!!.type.jvmErasure)
    assertEquals(B::class.java, B::function.instanceParameter!!.type.javaType)

    val property = B::class.members.single { it.name == "property" }
    val function = B::class.members.single { it.name == "function" }
    assertEquals(B::class, property.instanceParameter!!.type.jvmErasure)
    assertEquals(B::class.java, property.instanceParameter!!.type.javaType)
    assertEquals(B::class, function.instanceParameter!!.type.jvmErasure)
    assertEquals(B::class.java, function.instanceParameter!!.type.javaType)

    return "OK"
}
