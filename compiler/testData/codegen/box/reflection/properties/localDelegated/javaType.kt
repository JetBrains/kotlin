// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

object O {
    lateinit var property: KMutableProperty<*>
    operator fun provideDelegate(x: Any?, p: KProperty<*>): O {
        property = p as KMutableProperty<*>
        return this
    }
    operator fun getValue(x: Any?, p: KProperty<*>): String = "OK"
    operator fun setValue(x: Any?, p: KProperty<*>, value: String) {}
}

fun box(): String {
    var p by O
    val r = O.property
    assertEquals(String::class.java, r.returnType.javaType)
    assertEquals(String::class.java, r.getter.returnType.javaType)
    assertEquals(Void.TYPE, r.setter.returnType.javaType)
    return "OK"
}
