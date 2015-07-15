import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlin

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("Klass", Klass::class.jvmName)
    assertEquals("Klass\$Nested", Klass.Nested::class.jvmName)
    assertEquals("Klass\$Companion", Klass.Companion::class.jvmName)

    assertEquals("java.lang.Object", Any::class.jvmName)
    assertEquals("int", Int::class.jvmName)
    assertEquals("[I", IntArray::class.jvmName)
    assertEquals("java.util.List", List::class.jvmName)
    assertEquals("java.util.List", MutableList::class.jvmName)
    assertEquals("java.lang.String", String::class.jvmName)
    assertEquals("java.lang.String", java.lang.String::class.jvmName)

    assertEquals("[Ljava.lang.Object;", Array<Any>::class.jvmName)
    assertEquals("[Ljava.lang.Integer;", Array<Int>::class.jvmName)
    assertEquals("[[Ljava.lang.String;", Array<Array<String>>::class.jvmName)

    assertEquals("java.util.Date", java.util.Date::class.jvmName)
    assertEquals("kotlin.jvm.internal.KotlinSyntheticClass\$Kind", kotlin.jvm.internal.KotlinSyntheticClass.Kind::class.jvmName)
    assertEquals("java.lang.Void", java.lang.Void::class.jvmName)

    class Local
    val l = Local::class.jvmName
    assertTrue(l != null && l.startsWith("_DefaultPackage\$") && "\$box\$" in l && l.endsWith("\$Local"))

    val obj = object {}
    val o = obj.javaClass.kotlin.jvmName
    assertTrue(o != null && o.startsWith("_DefaultPackage\$") && "\$box\$" in o && o.endsWith("\$1"))

    return "OK"
}
