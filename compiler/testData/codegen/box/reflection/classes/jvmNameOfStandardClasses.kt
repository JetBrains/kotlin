// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.jvm.jvmName

fun box(): String {
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
    assertEquals("kotlin.jvm.internal.Ref\$ObjectRef", kotlin.jvm.internal.Ref.ObjectRef::class.jvmName)
    assertEquals("java.lang.Void", java.lang.Void::class.jvmName)

    return "OK"
}
