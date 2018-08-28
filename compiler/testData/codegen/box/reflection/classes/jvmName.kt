// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.jvm.jvmName

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("Klass", Klass::class.jvmName)
    assertEquals("Klass\$Nested", Klass.Nested::class.jvmName)
    assertEquals("Klass\$Companion", Klass.Companion::class.jvmName)

    class Local
    val l = Local::class.jvmName
    assertTrue(l != null && l.startsWith("JvmNameKt\$") && "\$box\$" in l && l.endsWith("\$Local"))

    val obj = object {}
    val o = obj.javaClass.kotlin.jvmName
    assertTrue(o != null && o.startsWith("JvmNameKt\$") && "\$box\$" in o && o.endsWith("\$1"))

    return "OK"
}
