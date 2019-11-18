// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

package test

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.jvm.jvmName

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("test.Klass", Klass::class.jvmName)
    assertEquals("test.Klass\$Nested", Klass.Nested::class.jvmName)
    assertEquals("test.Klass\$Companion", Klass.Companion::class.jvmName)

    class Local
    val l = Local::class.jvmName
    assertTrue(l != null && l.startsWith("test.JvmNameKt\$") && "\$box\$" in l && l.endsWith("\$Local"))

    val obj = object {}
    val o = obj.javaClass.kotlin.jvmName
    assertTrue(o != null && o.startsWith("test.JvmNameKt\$") && "\$box\$" in o && o.endsWith("\$1"))

    return "OK"
}
