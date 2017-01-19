// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("Klass", Klass::class.qualifiedName)
    assertEquals("Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("Klass.Companion", Klass.Companion::class.qualifiedName)

    assertEquals("kotlin.Any", Any::class.qualifiedName)
    assertEquals("kotlin.Int", Int::class.qualifiedName)
    assertEquals("kotlin.Int.Companion", Int.Companion::class.qualifiedName)
    assertEquals("kotlin.IntArray", IntArray::class.qualifiedName)
    assertEquals("kotlin.collections.List", List::class.qualifiedName)
    assertEquals("kotlin.String", String::class.qualifiedName)
    assertEquals("kotlin.String", java.lang.String::class.qualifiedName)

    assertEquals("kotlin.Array", Array<Any>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<Int>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<Array<String>>::class.qualifiedName)

    assertEquals("java.util.Date", java.util.Date::class.qualifiedName)
    assertEquals("kotlin.jvm.internal.Ref.ObjectRef", kotlin.jvm.internal.Ref.ObjectRef::class.qualifiedName)
    assertEquals("java.lang.Void", java.lang.Void::class.qualifiedName)

    class Local
    assertEquals(null, Local::class.qualifiedName)

    val o = object {}
    assertEquals(null, o.javaClass.kotlin.qualifiedName)

    return "OK"
}
